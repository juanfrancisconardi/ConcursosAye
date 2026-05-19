package ar.gov.entrerios.cge.concursos.domain.usecase

import ar.gov.entrerios.cge.concursos.domain.repository.ConcursoRepository
import javax.inject.Inject

/** Descarga el detalle completo si el aviso se guardó en modo rápido (solo listado). */
class EnsureConcursoDetailUseCase @Inject constructor(
    private val repository: ConcursoRepository
) {
    suspend operator fun invoke(concursoId: Long) = repository.ensureDetailLoaded(concursoId)
}
