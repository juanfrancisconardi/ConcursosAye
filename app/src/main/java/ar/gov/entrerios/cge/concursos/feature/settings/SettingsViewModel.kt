package ar.gov.entrerios.cge.concursos.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.gov.entrerios.cge.concursos.core.model.AppSettings
import ar.gov.entrerios.cge.concursos.core.model.Category
import ar.gov.entrerios.cge.concursos.core.model.DarkMode
import ar.gov.entrerios.cge.concursos.core.model.SyncMode
import ar.gov.entrerios.cge.concursos.domain.usecase.ObserveSettingsUseCase
import ar.gov.entrerios.cge.concursos.domain.usecase.RunForegroundSyncUseCase
import ar.gov.entrerios.cge.concursos.domain.usecase.UpdateSettingsUseCase
import ar.gov.entrerios.cge.concursos.work.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    observe: ObserveSettingsUseCase,
    private val updateSettings: UpdateSettingsUseCase,
    private val scheduler: SyncScheduler,
    private val runForegroundSync: RunForegroundSyncUseCase
) : ViewModel() {

    val settings: StateFlow<AppSettings> = observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    fun setSyncMode(mode: SyncMode) = update { it.copy(syncMode = mode) }

    fun setDailyTime(hour: Int, minute: Int) = update {
        it.copy(dailyHour = hour.coerceIn(0, 23), dailyMinute = minute.coerceIn(0, 59))
    }

    fun setNotifications(enabled: Boolean) = update { it.copy(notificationsEnabled = enabled) }
    fun setDarkMode(mode: DarkMode) = update { it.copy(darkMode = mode) }

    fun setSyncDaysBack(days: Int) = update {
        it.copy(syncDaysBack = days)
    }

    fun toggleCategory(category: Category) = update {
        val newSet = if (it.monitoredCategories.contains(category)) {
            it.monitoredCategories - category
        } else {
            it.monitoredCategories + category
        }
        it.copy(monitoredCategories = newSet)
    }

    fun forceRefreshNow() {
        viewModelScope.launch { runForegroundSync() }
    }

    private fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            val current = settings.value
            val next = transform(current)
            updateSettings(next)
            applySchedulingIfChanged(current, next)
        }
    }

    private fun applySchedulingIfChanged(prev: AppSettings, next: AppSettings) {
        val modeChanged = prev.syncMode != next.syncMode
        val timeChanged = prev.dailyHour != next.dailyHour || prev.dailyMinute != next.dailyMinute
        if (!modeChanged && !timeChanged) return

        when (next.syncMode) {
            SyncMode.ON_APP_OPEN -> scheduler.cancelPeriodic()
            SyncMode.DAILY -> scheduler.scheduleDaily(next.dailyHour, next.dailyMinute)
        }
    }
}
