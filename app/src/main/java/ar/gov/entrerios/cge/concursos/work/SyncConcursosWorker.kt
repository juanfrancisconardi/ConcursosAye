package ar.gov.entrerios.cge.concursos.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ar.gov.entrerios.cge.concursos.domain.repository.KeywordRepository
import ar.gov.entrerios.cge.concursos.domain.repository.SettingsRepository
import ar.gov.entrerios.cge.concursos.domain.usecase.SyncConcursosUseCase
import ar.gov.entrerios.cge.concursos.notifications.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

/**
 * Worker que se ejecuta periódicamente para descargar concursos nuevos y
 * disparar notificaciones de los relevantes.
 */
@HiltWorker
class SyncConcursosWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val syncConcursos: SyncConcursosUseCase,
    private val settingsRepository: SettingsRepository,
    private val keywordRepository: KeywordRepository,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        Timber.i("SyncConcursosWorker: comenzando sincronización")
        return try {
            // Asegurarnos de que el usuario nuevo tenga keywords por defecto
            keywordRepository.seedDefaultsIfEmpty()

            val report = syncConcursos()
            Timber.i(
                "Sync completado: %d listados, %d nuevos relevantes, %d errores",
                report.totalFetched,
                report.newRelevant.size,
                report.errors.size
            )

            val settings = settingsRepository.get()
            if (settings.notificationsEnabled && report.newRelevant.isNotEmpty()) {
                notificationHelper.notifyNewConcursos(report.newRelevant)
            }
            Result.success()
        } catch (t: Throwable) {
            Timber.e(t, "Sync falló")
            if (runAttemptCount >= MAX_RETRIES) Result.failure() else Result.retry()
        }
    }

    companion object {
        private const val MAX_RETRIES = 3
    }
}
