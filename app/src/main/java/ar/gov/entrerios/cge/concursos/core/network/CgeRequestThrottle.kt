package ar.gov.entrerios.cge.concursos.core.network

import ar.gov.entrerios.cge.concursos.core.util.Constants
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CgeRequestThrottle @Inject constructor(
    private val accessGuard: CgeAccessGuard
) {

    private val mutex = Mutex()
    private var lastRequestAtMs: Long = 0L

    suspend fun <T> run(block: suspend () -> T): T =
        runWith(Constants.SYNC_MIN_REQUEST_INTERVAL_MS, block)

    /**
     * Igual que [run] pero permite un intervalo menor para descargas de archivos
     * estáticos (adjuntos en /wp-content/uploads), que son menos sensibles a Wordfence.
     */
    suspend fun <T> runWith(intervalMs: Long, block: suspend () -> T): T = mutex.withLock {
        if (accessGuard.isBlocked()) {
            throw CgeAccessException(accessGuard.blockedMessage())
        }

        val now = System.currentTimeMillis()
        val wait = intervalMs - (now - lastRequestAtMs)
        if (wait > 0) delay(wait)

        try {
            block()
        } catch (e: CgeAccessException) {
            accessGuard.markBlocked()
            throw e
        } finally {
            lastRequestAtMs = System.currentTimeMillis()
        }
    }
}
