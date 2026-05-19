package ar.gov.entrerios.cge.concursos.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.gov.entrerios.cge.concursos.core.model.Concurso
import ar.gov.entrerios.cge.concursos.core.model.SyncMode
import ar.gov.entrerios.cge.concursos.core.util.SyncEventBus
import ar.gov.entrerios.cge.concursos.domain.usecase.ClearAllNewFlagsUseCase
import ar.gov.entrerios.cge.concursos.domain.usecase.ObserveRelevantConcursosUseCase
import ar.gov.entrerios.cge.concursos.domain.usecase.ObserveSettingsUseCase
import ar.gov.entrerios.cge.concursos.domain.usecase.RunForegroundSyncUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    observeRelevant: ObserveRelevantConcursosUseCase,
    observeSettings: ObserveSettingsUseCase,
    private val clearAllNewFlags: ClearAllNewFlagsUseCase,
    private val runForegroundSync: RunForegroundSyncUseCase,
    syncEventBus: SyncEventBus
) : ViewModel() {

    val concursos: StateFlow<List<Concurso>> =
        observeRelevant().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val syncMode: StateFlow<SyncMode> = observeSettings()
        .map { it.syncMode }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SyncMode.ON_APP_OPEN)

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val syncInProgress: StateFlow<Boolean> = syncEventBus.syncInProgress

    fun refresh() {
        if (_isRefreshing.value || syncInProgress.value) return
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
