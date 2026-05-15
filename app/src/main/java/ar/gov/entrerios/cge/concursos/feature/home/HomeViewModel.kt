package ar.gov.entrerios.cge.concursos.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.gov.entrerios.cge.concursos.core.model.Concurso
import ar.gov.entrerios.cge.concursos.domain.usecase.ClearAllNewFlagsUseCase
import ar.gov.entrerios.cge.concursos.domain.usecase.ObserveRelevantConcursosUseCase
import ar.gov.entrerios.cge.concursos.domain.usecase.RunForegroundSyncUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    observeRelevant: ObserveRelevantConcursosUseCase,
    private val clearAllNewFlags: ClearAllNewFlagsUseCase,
    private val runForegroundSync: RunForegroundSyncUseCase
) : ViewModel() {

    val concursos: StateFlow<List<Concurso>> =
        observeRelevant().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun refresh() {
        if (_isRefreshing.value) return
        _isRefreshing.value = true
        viewModelScope.launch {
            try {
                runForegroundSync()
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun clearNewFlags() {
        viewModelScope.launch { clearAllNewFlags() }
    }
}
