package ar.gov.entrerios.cge.concursos.domain.repository

import ar.gov.entrerios.cge.concursos.core.model.Keyword
import kotlinx.coroutines.flow.Flow

interface KeywordRepository {
    fun observeAll(): Flow<List<Keyword>>
    suspend fun getActive(): List<Keyword>
    suspend fun add(text: String): Long
    suspend fun update(keyword: Keyword)
    suspend fun delete(id: Long)
    suspend fun setEnabled(id: Long, enabled: Boolean)
    suspend fun seedDefaultsIfEmpty()
}
