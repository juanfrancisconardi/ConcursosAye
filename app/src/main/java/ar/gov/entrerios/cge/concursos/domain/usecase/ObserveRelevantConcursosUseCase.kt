package ar.gov.entrerios.cge.concursos.domain.usecase

import ar.gov.entrerios.cge.concursos.core.model.Concurso
import ar.gov.entrerios.cge.concursos.domain.repository.ConcursoRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveRelevantConcursosUseCase @Inject constructor(
    private val repository: ConcursoRepository
) {
    operator fun invoke(): Flow<List<Concurso>> = repository.observeRelevant()
}
