package ar.gov.entrerios.cge.concursos.feature.results

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.gov.entrerios.cge.concursos.core.model.Concurso
import ar.gov.entrerios.cge.concursos.core.util.TextNormalizer
import ar.gov.entrerios.cge.concursos.domain.usecase.ObserveAllConcursosUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ResultsViewModel @Inject constructor(
    observeAll: ObserveAllConcursosUseCase
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val concursos: StateFlow<List<Concurso>> =
        observeAll().combine(_query) { list, query ->
            val q = TextNormalizer.normalize(query)
            if (q.isBlank()) list
            else list.filter { c ->
                TextNormalizer.normalize(c.title).contains(q) ||
                    TextNormalizer.normalize(c.content).contains(q) ||
                    TextNormalizer.normalize(c.excerpt).contains(q)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onQueryChange(q: String) { _query.value = q }
}
