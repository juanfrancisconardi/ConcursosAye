package ar.gov.entrerios.cge.concursos.data.repository

import ar.gov.entrerios.cge.concursos.core.database.dao.ConcursoDao
import ar.gov.entrerios.cge.concursos.core.database.entity.ConcursoEntity
import ar.gov.entrerios.cge.concursos.core.model.Category
import ar.gov.entrerios.cge.concursos.core.model.Concurso
import ar.gov.entrerios.cge.concursos.core.network.CgeAccessException
import ar.gov.entrerios.cge.concursos.core.network.CgeAccessGuard
import ar.gov.entrerios.cge.concursos.core.network.CgeScraper
import ar.gov.entrerios.cge.concursos.core.network.dto.ConcursoListDto
import ar.gov.entrerios.cge.concursos.core.util.ConcursoDateFilter
import ar.gov.entrerios.cge.concursos.core.util.Constants
import ar.gov.entrerios.cge.concursos.core.util.KeywordMatcher
import ar.gov.entrerios.cge.concursos.data.mapper.toDomain
import ar.gov.entrerios.cge.concursos.data.mapper.toEntity
import ar.gov.entrerios.cge.concursos.domain.repository.ConcursoRepository
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
    private val accessGuard: CgeAccessGuard
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

        // 1) Una sola petición al índice /concursos/ (~90 avisos recientes de todos los niveles).
        try {
            val list = scraper.fetchConcursosFeed()
                .filter { item -> item.category == Category.GENERAL || item.category in categories }

            for (item in list) {
                if (!ConcursoDateFilter.isListItemWithinDays(item, publishedCutoff)) {
                    skippedByAge++
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
            totalFetched = list.size
            Timber.d("Sync índice: %d avisos, %d nuevos", list.size, newToFetch.size)
        } catch (e: CgeAccessException) {
            accessGuard.markBlocked()
            errors += e.message ?: CgeAccessException.MSG_DEFAULT
        } catch (t: Throwable) {
            Timber.w(t, "Error listando índice CGE")
            errors += t.message ?: t::class.java.simpleName
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

        // 3) Re-evaluar keywords solo en lo que sigue publicado en la 1.ª página del CGE
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

        lastSyncFinishedAtMs = System.currentTimeMillis()
        return SyncReport(
            totalFetched = totalFetched,
            newRelevant = newlyRelevant,
            errors = errors
        )
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
