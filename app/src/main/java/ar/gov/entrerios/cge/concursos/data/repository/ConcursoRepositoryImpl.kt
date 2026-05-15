package ar.gov.entrerios.cge.concursos.data.repository

import ar.gov.entrerios.cge.concursos.core.database.dao.ConcursoDao
import ar.gov.entrerios.cge.concursos.core.database.entity.ConcursoEntity
import ar.gov.entrerios.cge.concursos.core.model.Category
import ar.gov.entrerios.cge.concursos.core.model.Concurso
import ar.gov.entrerios.cge.concursos.core.network.CgeScraper
import ar.gov.entrerios.cge.concursos.core.network.dto.ConcursoDetailDto
import ar.gov.entrerios.cge.concursos.core.network.dto.ConcursoListDto
import ar.gov.entrerios.cge.concursos.core.util.KeywordMatcher
import ar.gov.entrerios.cge.concursos.data.mapper.toDomain
import ar.gov.entrerios.cge.concursos.data.mapper.toEntity
import ar.gov.entrerios.cge.concursos.domain.repository.ConcursoRepository
import ar.gov.entrerios.cge.concursos.domain.repository.KeywordRepository
import ar.gov.entrerios.cge.concursos.domain.repository.SettingsRepository
import ar.gov.entrerios.cge.concursos.domain.repository.SyncReport
import kotlinx.coroutines.flow.Flow
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
    private val settingsRepository: SettingsRepository
) : ConcursoRepository {

    override fun observeAll(): Flow<List<Concurso>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeRelevant(): Flow<List<Concurso>> =
        dao.observeRelevant().map { list -> list.map { it.toDomain() } }

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

    override suspend fun sync(): SyncReport {
        val settings = settingsRepository.get()
        val categories = settings.monitoredCategories.ifEmpty { Category.monitored.toSet() }
        val keywords = keywordRepository.getActive()
        val errors = mutableListOf<String>()
        val newlyRelevant = mutableListOf<Concurso>()
        var totalFetched = 0

        // 1) Listar todas las categorías
        val aggregated = LinkedHashMap<String, ConcursoListDto>()
        for (category in categories) {
            try {
                val list = scraper.fetchList(category)
                totalFetched += list.size
                for (item in list) {
                    aggregated.putIfAbsent(item.url, item)
                }
            } catch (t: Throwable) {
                Timber.w(t, "Error listando categoría %s", category.slug)
                errors += "Listado ${category.displayName}: ${t.message}"
            }
        }

        // 2) Procesar cada item: descargar detalle solo si es nuevo
        val now = System.currentTimeMillis()
        for ((url, listItem) in aggregated) {
            try {
                val existingId = dao.idByUrl(url)
                val isNew = existingId == null

                // Caché: si ya está y publishedAt no cambió respecto del listado, recalculamos
                // matches por si el usuario modificó keywords, pero NO volvemos a descargar.
                if (!isNew) {
                    recomputeMatchesForExisting(existingId, keywords)
                    continue
                }

                // Descargar detalle solo para urls nuevas
                val detail: ConcursoDetailDto = try {
                    scraper.fetchDetail(url, listItem.category)
                } catch (t: Throwable) {
                    Timber.w(t, "Error detalle %s, usando excerpt", url)
                    ConcursoDetailDto(
                        url = url,
                        title = listItem.title,
                        publishedAt = listItem.publishedAt,
                        content = listItem.excerpt,
                        category = listItem.category
                    )
                }

                val matches = KeywordMatcher.match(detail.title, detail.content, keywords)
                val score = KeywordMatcher.totalScore(matches)
                val contentHash = sha1(detail.content)

                val inserted = ConcursoEntity(
                    url = url,
                    title = detail.title,
                    publishedAt = detail.publishedAt ?: listItem.publishedAt,
                    categorySlug = detail.category.slug,
                    excerpt = listItem.excerpt,
                    content = detail.content,
                    contentHash = contentHash,
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
                errors += "Detalle $url: ${t.message}"
            }
        }

        return SyncReport(
            totalFetched = totalFetched,
            newRelevant = newlyRelevant,
            errors = errors
        )
    }

    /**
     * Re-aplica el matching con las keywords actuales para un concurso ya descargado.
     * Esto permite que cambios en las keywords reflejen score sobre histórico
     * sin necesidad de re-bajar el contenido.
     */
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
