package ar.gov.entrerios.cge.concursos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.gov.entrerios.cge.concursos.core.model.DarkMode
import ar.gov.entrerios.cge.concursos.core.model.SyncMode
import ar.gov.entrerios.cge.concursos.core.util.SyncEventBus
import ar.gov.entrerios.cge.concursos.core.util.UiSyncEvent
import ar.gov.entrerios.cge.concursos.domain.repository.SettingsRepository
import ar.gov.entrerios.cge.concursos.domain.usecase.ObserveSettingsUseCase
import ar.gov.entrerios.cge.concursos.domain.usecase.RunForegroundSyncUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    observeSettings: ObserveSettingsUseCase,
    private val settingsRepository: SettingsRepository,
    private val runForegroundSync: RunForegroundSyncUseCase,
    syncEventBus: SyncEventBus
) : ViewModel() {

    val darkMode: StateFlow<DarkMode> = observeSettings()
        .map { it.darkMode }
        .stateIn(viewModelScope, SharingStarted.Eagerly, DarkMode.SYSTEM)

    val syncEvents: SharedFlow<UiSyncEvent> = syncEventBus.events

    /**
     * Ejecuta una sincronización si el usuario eligió "Solo al abrir la app".
     * Se llama una sola vez por sesión desde [MainActivity.onCreate].
     */
    fun maybeRunSyncOnAppOpen() {
        viewModelScope.launch {
            val settings = settingsRepository.get()
            if (settings.syncMode == SyncMode.ON_APP_OPEN) {
                runForegroundSync()
            }
        }
    }
}
