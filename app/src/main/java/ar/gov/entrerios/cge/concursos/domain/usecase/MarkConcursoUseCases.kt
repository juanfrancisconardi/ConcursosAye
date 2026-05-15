package ar.gov.entrerios.cge.concursos.domain.usecase

import ar.gov.entrerios.cge.concursos.domain.repository.ConcursoRepository
import javax.inject.Inject

class MarkConcursoAsReadUseCase @Inject constructor(
    private val repository: ConcursoRepository
) {
    suspend operator fun invoke(id: Long) = repository.markAsRead(id)
}

class ClearAllNewFlagsUseCase @Inject constructor(
    private val repository: ConcursoRepository
) {
    suspend operator fun invoke() = repository.clearAllNewFlags()
}
