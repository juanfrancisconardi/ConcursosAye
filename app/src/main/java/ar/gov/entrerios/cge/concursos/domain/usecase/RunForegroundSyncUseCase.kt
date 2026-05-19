package ar.gov.entrerios.cge.concursos.domain.usecase

import ar.gov.entrerios.cge.concursos.core.network.CgeAccessException
import ar.gov.entrerios.cge.concursos.core.util.SyncEventBus
import ar.gov.entrerios.cge.concursos.domain.repository.ConcursoRepository
import ar.gov.entrerios.cge.concursos.domain.repository.SyncReport
import timber.log.Timber
import javax.inject.Inject

/**
 * Sincroniza concursos **en foreground** (mientras el usuario tiene la app abierta)
 * y notifica el resultado por el [SyncEventBus] para que la UI muestre el popup.
 *
 * A diferencia de [SyncConcursosUseCase], además:
 *  - emite eventos al bus (Started / Completed / Failed)
 *  - atrapa excepciones para que la UI siempre reciba un reporte
 */
class RunForegroundSyncUseCase @Inject constructor(
    private val repository: ConcursoRepository,
    private val syncEventBus: SyncEventBus
) {
    suspend operator fun invoke(): SyncReport {
        syncEventBus.publishStart()
        return try {
            val report = repository.sync()
            syncEventBus.publishResult(report)
            report
        } catch (t: Throwable) {
            Timber.e(t, "Foreground sync falló")
            syncEventBus.publishError(t)
            val msg = when (t) {
                is CgeAccessException -> t.message ?: CgeAccessException.MSG_DEFAULT
                else -> t.message ?: t::class.java.simpleName
            }
            SyncReport(
                totalFetched = 0,
                newRelevant = emptyList(),
                errors = listOf(msg)
            )
        }
    }
}
