package ar.gov.entrerios.cge.concursos

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import ar.gov.entrerios.cge.concursos.core.model.SyncMode
import ar.gov.entrerios.cge.concursos.domain.repository.ConcursoRepository
import ar.gov.entrerios.cge.concursos.domain.repository.KeywordRepository
import ar.gov.entrerios.cge.concursos.domain.repository.SettingsRepository
import ar.gov.entrerios.cge.concursos.notifications.NotificationHelper
import ar.gov.entrerios.cge.concursos.work.SyncScheduler
import com.google.android.gms.ads.MobileAds
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class ConcursosApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var syncScheduler: SyncScheduler
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var keywordRepository: KeywordRepository
    @Inject lateinit var concursoRepository: ConcursoRepository
    @Inject lateinit var notificationHelper: NotificationHelper

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())

        MobileAds.initialize(this)

        notificationHelper.ensureChannel()

        appScope.launch {
            keywordRepository.seedDefaultsIfEmpty()
            // Limpia scores/coincidencias viejos para que "Relevantes" refleje
            // siempre las keywords activas actuales (evita resultados fantasma).
            concursoRepository.recomputeAllRelevance()
            applySyncMode()
        }
    }

    private suspend fun applySyncMode() {
        val settings = settingsRepository.get()
        when (settings.syncMode) {
            SyncMode.DAILY -> syncScheduler.scheduleDaily(settings.dailyHour, settings.dailyMinute)
            SyncMode.ON_APP_OPEN -> syncScheduler.cancelPeriodic()
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) android.util.Log.DEBUG else android.util.Log.INFO)
            .build()
}
