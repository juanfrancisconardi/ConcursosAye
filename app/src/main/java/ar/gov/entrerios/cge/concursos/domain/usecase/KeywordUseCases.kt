package ar.gov.entrerios.cge.concursos.domain.usecase

import ar.gov.entrerios.cge.concursos.core.model.Keyword
import ar.gov.entrerios.cge.concursos.domain.repository.ConcursoRepository
import ar.gov.entrerios.cge.concursos.domain.repository.KeywordRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveKeywordsUseCase @Inject constructor(
    private val repository: KeywordRepository
) {
    operator fun invoke(): Flow<List<Keyword>> = repository.observeAll()
}

class AddKeywordUseCase @Inject constructor(
    private val repository: KeywordRepository,
    private val concursoRepository: ConcursoRepository
) {
    suspend operator fun invoke(text: String): Long {
        val id = repository.add(text)
        if (id > 0) concursoRepository.recomputeAllRelevance()
        return id
    }
}

class UpdateKeywordUseCase @Inject constructor(
    private val repository: KeywordRepository,
    private val concursoRepository: ConcursoRepository
) {
    suspend operator fun invoke(keyword: Keyword) {
        repository.update(keyword)
        concursoRepository.recomputeAllRelevance()
    }
}

class DeleteKeywordUseCase @Inject constructor(
    private val repository: KeywordRepository,
    private val concursoRepository: ConcursoRepository
) {
    suspend operator fun invoke(id: Long) {
        repository.delete(id)
        concursoRepository.recomputeAllRelevance()
    }
}

class ToggleKeywordUseCase @Inject constructor(
    private val repository: KeywordRepository,
    private val concursoRepository: ConcursoRepository
) {
    suspend operator fun invoke(id: Long, enabled: Boolean) {
        repository.setEnabled(id, enabled)
        concursoRepository.recomputeAllRelevance()
    }
}
