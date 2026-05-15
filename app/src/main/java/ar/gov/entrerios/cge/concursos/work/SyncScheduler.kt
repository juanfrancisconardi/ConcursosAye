package ar.gov.entrerios.cge.concursos.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import ar.gov.entrerios.cge.concursos.core.util.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Programa una sincronización **diaria** a la hora indicada (formato 24h).
     *
     * Internamente usamos un [PeriodicWorkRequestBuilder] de 24 horas con un
     * `initialDelay` calculado hasta la próxima ocurrencia de `HH:MM`.
     */
    fun scheduleDaily(hour: Int, minute: Int) {
        val safeHour = hour.coerceIn(0, 23)
        val safeMinute = minute.coerceIn(0, 59)

        val initialDelayMillis = computeInitialDelayMillis(safeHour, safeMinute)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<SyncConcursosWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag(Constants.SYNC_WORK_TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            Constants.SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )

        Timber.i(
            "SyncScheduler: programado chequeo DIARIO a las %02d:%02d (initialDelay=%d ms)",
            safeHour, safeMinute, initialDelayMillis
        )
    }

    /**
     * Solicita una sincronización inmediata (refresh manual).
     * Esta opción se usa internamente cuando el VM no puede llamar al UseCase directo,
     * pero la app actualmente ejecuta sincronización foreground vía UseCase.
     */
    fun runOnce() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<SyncConcursosWorker>()
            .setConstraints(constraints)
            .addTag(Constants.SYNC_WORK_TAG)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "${Constants.SYNC_WORK_NAME}_once",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    /** Cancela cualquier sincronización periódica programada. */
    fun cancelPeriodic() {
        WorkManager.getInstance(context).cancelUniqueWork(Constants.SYNC_WORK_NAME)
        Timber.i("SyncScheduler: chequeo periódico CANCELADO (modo ON_APP_OPEN)")
    }

    /**
     * Calcula cuántos milisegundos faltan hasta la próxima ocurrencia de `HH:MM`.
     * Si la hora ya pasó hoy, devuelve los milisegundos hasta `HH:MM` de **mañana**.
     */
    internal fun computeInitialDelayMillis(
        hour: Int,
        minute: Int,
        now: Calendar = Calendar.getInstance()
    ): Long {
        val target = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (!target.after(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis - now.timeInMillis
    }
}
