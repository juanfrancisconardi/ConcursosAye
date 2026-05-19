package ar.gov.entrerios.cge.concursos.data.repository

import ar.gov.entrerios.cge.concursos.core.database.dao.KeywordDao
import ar.gov.entrerios.cge.concursos.core.database.entity.KeywordEntity
import ar.gov.entrerios.cge.concursos.core.model.Keyword
import ar.gov.entrerios.cge.concursos.core.util.TextNormalizer
import ar.gov.entrerios.cge.concursos.data.mapper.toDomain
import ar.gov.entrerios.cge.concursos.data.mapper.toEntity
import ar.gov.entrerios.cge.concursos.domain.repository.KeywordRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KeywordRepositoryImpl @Inject constructor(
    private val dao: KeywordDao
) : KeywordRepository {

    override fun observeAll(): Flow<List<Keyword>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getActive(): List<Keyword> =
        dao.getActive().map { it.toDomain() }

    override suspend fun add(text: String): Long {
        val clean = text.trim()
        if (clean.isEmpty()) return -1L

        val normalized = TextNormalizer.normalize(clean)
        val existing = dao.findByNormalized(normalized)
        if (existing != null) {
            dao.update(
                existing.copy(
                    text = clean,
                    enabled = true
                )
            )
            return existing.id
        }

        val insertedId = dao.insert(
            KeywordEntity(
                text = clean,
                normalizedText = normalized,
                enabled = true
            )
        )
        return if (insertedId == -1L) {
            dao.findByNormalized(normalized)?.id ?: -1L
        } else {
            insertedId
        }
    }

    override suspend fun update(keyword: Keyword) {
        dao.update(keyword.toEntity())
    }

    override suspend fun delete(id: Long) {
        dao.deleteById(id)
    }

    override suspend fun setEnabled(id: Long, enabled: Boolean) {
        dao.setEnabled(id, enabled)
    }

    override suspend fun seedDefaultsIfEmpty() {
        if (dao.getAll().isNotEmpty()) return
        val defaults = listOf(
            "terapista ocupacional",
            "psicólogo",
            "psicopedagogo",
            "fonoaudiólogo",
            "equipo técnico",
            "integración",
            "eoe",
            "saie",
            "ovo",
            "flo"
        )
        defaults.forEach { add(it) }
    }
}
