package ar.gov.entrerios.cge.concursos.feature.keywords

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.gov.entrerios.cge.concursos.core.model.Keyword
import ar.gov.entrerios.cge.concursos.domain.usecase.AddKeywordUseCase
import ar.gov.entrerios.cge.concursos.domain.usecase.DeleteKeywordUseCase
import ar.gov.entrerios.cge.concursos.domain.usecase.ObserveKeywordsUseCase
import ar.gov.entrerios.cge.concursos.domain.usecase.ToggleKeywordUseCase
import ar.gov.entrerios.cge.concursos.domain.usecase.UpdateKeywordUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class KeywordsViewModel @Inject constructor(
    observe: ObserveKeywordsUseCase,
    private val addUseCase: AddKeywordUseCase,
    private val updateUseCase: UpdateKeywordUseCase,
    private val deleteUseCase: DeleteKeywordUseCase,
    private val toggleUseCase: ToggleKeywordUseCase
) : ViewModel() {

    val keywords: StateFlow<List<Keyword>> = observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun add(text: String) = viewModelScope.launch {
        if (text.isNotBlank()) addUseCase(text)
    }

    fun update(keyword: Keyword) = viewModelScope.launch { updateUseCase(keyword) }

    fun delete(id: Long) = viewModelScope.launch { deleteUseCase(id) }

    fun toggle(id: Long, enabled: Boolean) = viewModelScope.launch { toggleUseCase(id, enabled) }
}
