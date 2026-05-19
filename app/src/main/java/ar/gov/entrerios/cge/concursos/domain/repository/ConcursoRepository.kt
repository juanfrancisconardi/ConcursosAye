package ar.gov.entrerios.cge.concursos.domain.repository

import ar.gov.entrerios.cge.concursos.core.model.Concurso
import kotlinx.coroutines.flow.Flow

interface ConcursoRepository {

    fun observeAll(): Flow<List<Concurso>>
    fun observeRelevant(): Flow<List<Concurso>>
    fun observeById(id: Long): Flow<Concurso?>

    suspend fun getById(id: Long): Concurso?

    /**
     * Ejecuta una sincronización completa:
     *  1. Descarga listados de cada categoría monitoreada.
     *  2. Para cada URL nueva, guarda título/fecha y aplica keywords (detalle bajo demanda).
     *  3. Re-aplica el matching con las keywords actuales.
     *
     * @return cantidad de concursos NUEVOS y RELEVANTES detectados en esta corrida.
     */
    suspend fun sync(): SyncReport

    /** Completa título/cuerpo desde el sitio si la sync rápida no bajó el detalle. */
    suspend fun ensureDetailLoaded(id: Long)

    suspend fun markAsRead(id: Long)
    suspend fun markAsSeen(id: Long)
    suspend fun clearAllNewFlags()
}

data class SyncReport(
    val totalFetched: Int,
    val newRelevant: List<Concurso>,
    val errors: List<String>
)
