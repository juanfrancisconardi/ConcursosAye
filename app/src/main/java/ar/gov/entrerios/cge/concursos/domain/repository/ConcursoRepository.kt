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

    /** Recalcula score y coincidencias de TODOS los avisos según keywords activas actuales. */
    suspend fun recomputeAllRelevance()

    /**
     * Lectura profunda de un aviso: baja sus adjuntos (imágenes/PDF), les aplica OCR,
     * suma ese texto al contenido buscable y re-evalúa las keywords.
     */
    suspend fun deepScanConcurso(id: Long)

    /** Lectura profunda de los avisos recientes aún no escaneados ("Buscar a fondo"). */
    suspend fun deepScanRecent(): DeepScanReport

    suspend fun markAsRead(id: Long)
    suspend fun markAsSeen(id: Long)
    suspend fun clearAllNewFlags()
}

data class SyncReport(
    val totalFetched: Int,
    val newRelevant: List<Concurso>,
    val errors: List<String>
)

data class DeepScanReport(
    val processed: Int,
    val newlyRelevant: Int,
    val blocked: Boolean,
    val error: String? = null
)
