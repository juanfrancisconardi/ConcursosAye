package ar.gov.entrerios.cge.concursos.data.repository

import ar.gov.entrerios.cge.concursos.core.database.dao.ConcursoDao
import ar.gov.entrerios.cge.concursos.core.database.entity.ConcursoEntity
import ar.gov.entrerios.cge.concursos.core.model.Category
import ar.gov.entrerios.cge.concursos.core.model.Concurso
import ar.gov.entrerios.cge.concursos.core.model.Departamental
import ar.gov.entrerios.cge.concursos.core.network.CgeAccessException
import ar.gov.entrerios.cge.concursos.core.network.CgeAccessGuard
import ar.gov.entrerios.cge.concursos.core.network.findCgeAccessException
import ar.gov.entrerios.cge.concursos.core.network.CgeScraper
import ar.gov.entrerios.cge.concursos.core.network.dto.ConcursoListDto
import ar.gov.entrerios.cge.concursos.core.ocr.AttachmentTextExtractor
import ar.gov.entrerios.cge.concursos.core.util.ConcursoDateFilter
import ar.gov.entrerios.cge.concursos.core.util.Constants
import ar.gov.entrerios.cge.concursos.core.util.KeywordMatcher
import ar.gov.entrerios.cge.concursos.data.mapper.toDomain
import ar.gov.entrerios.cge.concursos.data.mapper.toEntity
import ar.gov.entrerios.cge.concursos.domain.repository.ConcursoRepository
import ar.gov.entrerios.cge.concursos.domain.repository.DeepScanReport
import ar.gov.entrerios.cge.concursos.domain.repository.KeywordRepository
import ar.gov.entrerios.cge.concursos.domain.repository.SettingsRepository
import ar.gov.entrerios.cge.concursos.domain.repository.SyncReport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConcursoRepositoryImpl @Inject constructor(
    private val dao: ConcursoDao,
    private val scraper: CgeScraper,
    private val keywordRepository: KeywordRepository,
    private val settingsRepository: SettingsRepository,
    private val accessGuard: CgeAccessGuard,
    private val attachmentTextExtractor: AttachmentTextExtractor
) : ConcursoRepository {

    private val syncMutex = Mutex()
    private var lastSyncFinishedAtMs: Long = 0L

    override fun observeAll(): Flow<List<Concurso>> =
        combine(settingsRepository.observe(), dao.observeAll()) { settings, list ->
            ConcursoDateFilter.filterConcursos(
                list.map { it.toDomain() },
                settings.syncDaysBack
            )
        }

    override fun observeRelevant(): Flow<List<Concurso>> =
        combine(settingsRepository.observe(), dao.observeRelevant()) { settings, list ->
            ConcursoDateFilter.filterConcursos(
                list.map { it.toDomain() },
                settings.syncDaysBack
            )
        }

    override fun observeById(id: Long): Flow<Concurso?> =
        dao.observeById(id).map { it?.toDomain() }

    override suspend fun getById(id: Long): Concurso? =
        dao.getById(id)?.toDomain()

    override suspend fun markAsRead(id: Long) {
        dao.markAsRead(id)
    }

    override suspend fun markAsSeen(id: Long) {
        dao.markAsSeen(id)
    }

    override suspend fun clearAllNewFlags() {
        dao.clearAllNewFlags()
    }

    override suspend fun recomputeAllRelevance() {
        val keywords = keywordRepository.getActive()
        val concursos = dao.getAll()
        concursos.forEach { concurso ->
            val matches = KeywordMatcher.match(concurso.title, concurso.content, keywords)
            val newScore = KeywordMatcher.totalScore(matches)
            if (newScore != concurso.score) {
                dao.update(concurso.copy(score = newScore))
            }
            dao.replaceMatches(concurso.id, matches.map { it.toEntity(concurso.id) })
        }
    }

    override suspend fun deepScanConcurso(id: Long) {
        val existing = dao.getById(id)?.concurso ?: return
        if (accessGuard.isBlocked()) return

        val category = Category.fromSlug(existing.categorySlug)
        val (detail, attachments) = try {
            scraper.fetchDetailWithAttachments(existing.url, category)
        } catch (e: CgeAccessException) {
            accessGuard.markBlocked()
            throw e
        }

        val ocrText = attachmentTextExtractor.extractText(attachments)

        val baseContent = detail.content.ifBlank { existing.content }
        val combined = listOf(baseContent, ocrText)
            .filter { it.isNotBlank() }
            .joinToString("\n")
            .trim()

        val title = detail.title.ifBlank { existing.title }
        val keywords = keywordRepository.getActive()
        val matches = KeywordMatcher.match(title, combined, keywords)
        val score = KeywordMatcher.totalScore(matches)

        dao.update(
            existing.copy(
                title = title,
                publishedAt = detail.publishedAt ?: existing.publishedAt,
                categorySlug = detail.category.slug,
                content = combined,
                contentHash = if (combined.isBlank()) existing.contentHash else sha1(combined),
                score = score,
                deepScannedAt = System.currentTimeMillis()
            )
        )
        dao.replaceMatches(id, matches.map { it.toEntity(id) })
    }

    override suspend fun deepScanRecent(): DeepScanReport {
        if (accessGuard.isBlocked()) {
            return DeepScanReport(processed = 0, newlyRelevant = 0, blocked = true)
        }
        val settings = settingsRepository.get()
        val now = System.currentTimeMillis()
        val cutoff = ConcursoDateFilter.cutoffMillis(now, settings.syncDaysBack)

        val candidates = dao.getAll()
            .filter { it.deepScannedAt == null }
            .filter { ConcursoDateFilter.isWithinDays(it.publishedAt, it.detectedAt, cutoff) }
            .sortedByDescending { it.publishedAt ?: it.detectedAt }
            .take(Constants.DEEP_SCAN_MAX_POSTS)

        var processed = 0
        var newlyRelevant = 0
        for (candidate in candidates) {
            if (accessGuard.isBlocked()) {
                return DeepScanReport(processed, newlyRelevant, blocked = true)
            }
            val scoreBefore = candidate.score
            try {
                deepScanConcurso(candidate.id)
                processed++
                val scoreAfter = dao.getById(candidate.id)?.concurso?.score ?: 0
                if (scoreBefore <= 0 && scoreAfter > 0) newlyRelevant++
            } catch (e: CgeAccessException) {
                return DeepScanReport(processed, newlyRelevant, blocked = true, error = e.message)
            } catch (t: Throwable) {
                Timber.w(t, "Deep scan falló para id=%d", candidate.id)
            }
        }
        return DeepScanReport(processed, newlyRelevant, blocked = false)
    }

    override suspend fun sync(): SyncReport = syncMutex.withLock {
        syncInternal()
    }

    private suspend fun syncInternal(): SyncReport {
        val now = System.currentTimeMillis()
        if (accessGuard.isBlocked()) {
            return SyncReport(
                totalFetched = 0,
                newRelevant = emptyList(),
                errors = listOf(accessGuard.blockedMessage())
            )
        }
        if (now - lastSyncFinishedAtMs < Constants.SYNC_MIN_INTERVAL_BETWEEN_RUNS_MS) {
            val waitSec = (Constants.SYNC_MIN_INTERVAL_BETWEEN_RUNS_MS - (now - lastSyncFinishedAtMs)) / 1000
            return SyncReport(
                totalFetched = 0,
                newRelevant = emptyList(),
                errors = listOf("Esperá ${waitSec}s antes de sincronizar otra vez (el CGE limita consultas).")
            )
        }

        val settings = settingsRepository.get()
        val categories = settings.monitoredCategories.ifEmpty { Category.monitored.toSet() }
        val keywords = keywordRepository.getActive()
        val daysBack = settings.syncDaysBack
        val publishedCutoff = ConcursoDateFilter.cutoffMillis(now, daysBack)
        val errors = mutableListOf<String>()
        val newlyRelevant = mutableListOf<Concurso>()
        var totalFetched = 0
        var skippedByAge = 0

        val newToFetch = mutableListOf<ConcursoListDto>()
        val newUrlsQueued = mutableSetOf<String>()
        val listedNowIds = mutableSetOf<Long>()
        val newlyInsertedIds = mutableListOf<Long>()

        // 1) Índice general /concursos/
        totalFetched += ingestFeedSource(
            sourceLabel = "índice",
            fetch = { scraper.fetchConcursosFeed() },
            categories = categories,
            publishedCutoff = publishedCutoff,
            newToFetch = newToFetch,
            newUrlsQueued = newUrlsQueued,
            listedNowIds = listedNowIds,
            skippedByAge = { skippedByAge += it },
            errors = errors
        )

        // 1b) Página de la DDE elegida (misma estructura de convocatorias).
        val departamental = settings.selectedDepartamental
        if (departamental.isActive && !accessGuard.isBlocked()) {
            totalFetched += ingestFeedSource(
                sourceLabel = departamental.displayName,
                fetch = { scraper.fetchDepartamentalFeed(departamental) },
                categories = categories,
                publishedCutoff = publishedCutoff,
                newToFetch = newToFetch,
                newUrlsQueued = newUrlsQueued,
                listedNowIds = listedNowIds,
                skippedByAge = { skippedByAge += it },
                errors = errors
            )
        }

        Timber.d(
            "Sync (%d días): %d listados, %d omitidos por antigüedad, %d nuevos, %d en portada",
            daysBack,
            totalFetched,
            skippedByAge,
            newToFetch.size,
            listedNowIds.size
        )

        // 2) Guardar avisos nuevos con datos del listado (sin bajar detalle; más rápido).
        for (listItem in newToFetch) {
            val url = listItem.url
            try {
                val publishedAt = listItem.publishedAt
                if (publishedAt != null &&
                    !ConcursoDateFilter.isWithinDays(publishedAt, now, publishedCutoff)
                ) {
                    skippedByAge++
                    continue
                }

                val previewContent = listItem.excerpt
                val matches = KeywordMatcher.match(listItem.title, previewContent, keywords)
                val score = KeywordMatcher.totalScore(matches)

                val inserted = ConcursoEntity(
                    url = url,
                    title = listItem.title,
                    publishedAt = publishedAt,
                    categorySlug = listItem.category.slug,
                    excerpt = listItem.excerpt,
                    content = previewContent,
                    contentHash = Constants.CONTENT_HASH_PENDING,
                    detectedAt = now,
                    isNew = true,
                    isRead = false,
                    score = score
                )
                val newId = dao.insert(inserted)
                if (newId > 0) {
                    newlyInsertedIds += newId
                    dao.replaceMatches(newId, matches.map { it.toEntity(newId) })
                    if (score > 0) {
                        newlyRelevant += inserted.copy(id = newId).toDomain(matches)
                    }
                }
            } catch (t: Throwable) {
                Timber.e(t, "Error procesando %s", url)
                errors += "Guardar $url: ${t.message}"
            }
        }

        // 3) Re-evaluar keywords en avisos ya conocidos que siguen listados.
        for (id in listedNowIds) {
            try {
                val existing = dao.getById(id)?.concurso ?: continue
                if (!ConcursoDateFilter.isWithinDays(existing.publishedAt, existing.detectedAt, publishedCutoff)) {
                    continue
                }
                recomputeMatchesForExisting(id, keywords)
            } catch (t: Throwable) {
                Timber.w(t, "Error re-evaluando id=%d", id)
            }
        }

        // 4) Lectura profunda (OCR) de la DDE: avisos nuevos + pendientes recientes.
        if (departamental.isActive && !accessGuard.isBlocked()) {
            deepScanAfterDepartamentalSync(
                departamental = departamental,
                newlyInsertedIds = newlyInsertedIds,
                publishedCutoff = publishedCutoff,
                now = now,
                newlyRelevant = newlyRelevant,
                errors = errors
            )
        }

        lastSyncFinishedAtMs = System.currentTimeMillis()
        return SyncReport(
            totalFetched = totalFetched,
            newRelevant = newlyRelevant,
            errors = errors
        )
    }

    private suspend fun ingestFeedSource(
        sourceLabel: String,
        fetch: suspend () -> List<ConcursoListDto>,
        categories: Set<Category>,
        publishedCutoff: Long,
        newToFetch: MutableList<ConcursoListDto>,
        newUrlsQueued: MutableSet<String>,
        listedNowIds: MutableSet<Long>,
        skippedByAge: (Int) -> Unit,
        errors: MutableList<String>
    ): Int {
        return try {
            val list = fetch()
                .filter { item -> item.category == Category.GENERAL || item.category in categories }

            var skipped = 0
            for (item in list) {
                if (!ConcursoDateFilter.isListItemWithinDays(item, publishedCutoff)) {
                    skipped++
                    continue
                }
                when (val existingId = dao.idByUrl(item.url)) {
                    null -> {
                        if (newUrlsQueued.add(item.url)) {
                            newToFetch += item
                        }
                    }
                    else -> listedNowIds += existingId
                }
            }
            skippedByAge(skipped)
            Timber.d("Sync %s: %d avisos, %d nuevos en cola", sourceLabel, list.size, newToFetch.size)
            list.size
        } catch (e: CgeAccessException) {
            accessGuard.markBlocked()
            errors += e.message ?: CgeAccessException.MSG_DEFAULT
            0
        } catch (e: java.io.IOException) {
            val blocked = e.findCgeAccessException()
            if (blocked != null) {
                accessGuard.markBlocked()
                errors += blocked.message ?: CgeAccessException.MSG_DEFAULT
            } else {
                Timber.w(e, "Error de red listando %s", sourceLabel)
                errors += e.message ?: "Error de red ($sourceLabel)"
            }
            0
        } catch (t: Throwable) {
            Timber.w(t, "Error listando %s", sourceLabel)
            errors += t.message ?: t::class.java.simpleName
            0
        }
    }

    /**
     * Tras sincronizar una DDE, OCR-ea adjuntos de avisos nuevos y recientes sin escanear,
     * para detectar cargos que solo figuran en imágenes/PDF.
     */
    private suspend fun deepScanAfterDepartamentalSync(
        departamental: Departamental,
        newlyInsertedIds: List<Long>,
        publishedCutoff: Long,
        now: Long,
        newlyRelevant: MutableList<Concurso>,
        errors: MutableList<String>
    ) {
        val pendingExisting = dao.getAll()
            .filter { it.deepScannedAt == null && it.id !in newlyInsertedIds }
            .filter { ConcursoDateFilter.isWithinDays(it.publishedAt, it.detectedAt, publishedCutoff) }
            .sortedByDescending { it.publishedAt ?: it.detectedAt }
            .map { it.id }

        val targets = (newlyInsertedIds + pendingExisting)
            .distinct()
            .take(Constants.DEEP_SCAN_MAX_POSTS)

        Timber.d("Deep scan DDE %s: %d avisos", departamental.displayName, targets.size)

        for (id in targets) {
            if (accessGuard.isBlocked()) break
            val scoreBefore = dao.getById(id)?.concurso?.score ?: 0
            try {
                deepScanConcurso(id)
                val updated = dao.getById(id)?.toDomain() ?: continue
                if (scoreBefore <= 0 && updated.score > 0 && newlyRelevant.none { it.id == id }) {
                    newlyRelevant += updated
                }
            } catch (e: CgeAccessException) {
                errors += e.message ?: CgeAccessException.MSG_DEFAULT
                break
            } catch (t: Throwable) {
                Timber.w(t, "Deep scan post-sync falló id=%d", id)
            }
        }
    }

    override suspend fun ensureDetailLoaded(id: Long) {
        val row = dao.getById(id) ?: return
        val existing = row.concurso
        if (existing.contentHash != Constants.CONTENT_HASH_PENDING) return
        if (accessGuard.isBlocked()) return

        val category = Category.fromSlug(existing.categorySlug)
        val detail = try {
            scraper.fetchDetail(existing.url, category)
        } catch (e: CgeAccessException) {
            accessGuard.markBlocked()
            Timber.w(e, "Detalle bloqueado id=%d", id)
            return
        } catch (t: Throwable) {
            Timber.w(t, "No se pudo bajar detalle id=%d", id)
            return
        }

        val keywords = keywordRepository.getActive()
        val matches = KeywordMatcher.match(detail.title, detail.content, keywords)
        val score = KeywordMatcher.totalScore(matches)
        val updated = existing.copy(
            title = detail.title,
            publishedAt = detail.publishedAt ?: existing.publishedAt,
            categorySlug = detail.category.slug,
            content = detail.content,
            contentHash = sha1(detail.content),
            score = score
        )
        dao.update(updated)
        dao.replaceMatches(id, matches.map { it.toEntity(id) })
    }

    private suspend fun recomputeMatchesForExisting(
        id: Long,
        keywords: List<ar.gov.entrerios.cge.concursos.core.model.Keyword>
    ) {
        val existing = dao.getById(id)?.concurso ?: return
        val matches = KeywordMatcher.match(existing.title, existing.content, keywords)
        val newScore = KeywordMatcher.totalScore(matches)
        if (newScore != existing.score) {
            dao.update(existing.copy(score = newScore))
        }
        dao.replaceMatches(id, matches.map { it.toEntity(id) })
    }

    private fun sha1(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-1").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

}
