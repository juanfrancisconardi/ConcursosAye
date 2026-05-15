package ar.gov.entrerios.cge.concursos.domain.usecase

import ar.gov.entrerios.cge.concursos.domain.repository.ConcursoRepository
import ar.gov.entrerios.cge.concursos.domain.repository.SyncReport
import javax.inject.Inject

class SyncConcursosUseCase @Inject constructor(
    private val repository: ConcursoRepository
) {
    suspend operator fun invoke(): SyncReport = repository.sync()
}
