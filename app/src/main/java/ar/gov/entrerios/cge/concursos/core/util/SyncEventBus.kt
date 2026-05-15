package ar.gov.entrerios.cge.concursos.core.util

import ar.gov.entrerios.cge.concursos.domain.repository.SyncReport
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bus global (Singleton) por el cual se difunde el resultado de cada sincronización
 * realizada en foreground. La UI lo escucha para mostrar el popup de "reporte".
 *
 * Las sincronizaciones que corre el WorkManager en background NO emiten aquí
 * (ya generan notificación push); este bus es exclusivo para foreground.
 */
@Singleton
class SyncEventBus @Inject constructor() {

    private val _events = MutableSharedFlow<UiSyncEvent>(
        replay = 0,
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<UiSyncEvent> = _events.asSharedFlow()

    suspend fun publishStart() = _events.emit(UiSyncEvent.Started)
    suspend fun publishResult(report: SyncReport) = _events.emit(UiSyncEvent.Completed(report))
    suspend fun publishError(throwable: Throwable) = _events.emit(UiSyncEvent.Failed(throwable))
}

sealed interface UiSyncEvent {
    data object Started : UiSyncEvent
    data class Completed(val report: SyncReport) : UiSyncEvent
    data class Failed(val throwable: Throwable) : UiSyncEvent
}
