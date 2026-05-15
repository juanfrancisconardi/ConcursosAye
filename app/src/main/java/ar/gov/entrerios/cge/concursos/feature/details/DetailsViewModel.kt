package ar.gov.entrerios.cge.concursos.feature.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.gov.entrerios.cge.concursos.core.model.Concurso
import ar.gov.entrerios.cge.concursos.domain.usecase.MarkConcursoAsReadUseCase
import ar.gov.entrerios.cge.concursos.domain.usecase.ObserveConcursoDetailUseCase
import ar.gov.entrerios.cge.concursos.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeDetail: ObserveConcursoDetailUseCase,
    private val markAsRead: MarkConcursoAsReadUseCase
) : ViewModel() {

    private val concursoId: Long = savedStateHandle.get<Long>(Routes.DETAILS_ARG) ?: 0L

    val concurso: StateFlow<Concurso?> = observeDetail(concursoId)
        .onEach { it?.let { c -> if (!c.isRead) viewModelScope.launch { markAsRead(c.id) } } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
