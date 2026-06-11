package ar.gov.entrerios.cge.concursos.domain.usecase

import ar.gov.entrerios.cge.concursos.domain.repository.ConcursoRepository
import ar.gov.entrerios.cge.concursos.domain.repository.DeepScanReport
import javax.inject.Inject

/** Lee los adjuntos (imágenes/PDF) de un aviso con OCR y re-evalúa keywords. */
class DeepScanConcursoUseCase @Inject constructor(
    private val repository: ConcursoRepository
) {
    suspend operator fun invoke(concursoId: Long) = repository.deepScanConcurso(concursoId)
}

/** Corre la lectura profunda sobre los avisos recientes ("Buscar a fondo"). */
class DeepScanRecentUseCase @Inject constructor(
    private val repository: ConcursoRepository
) {
    suspend operator fun invoke(): DeepScanReport = repository.deepScanRecent()
}
