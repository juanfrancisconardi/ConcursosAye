package ar.gov.entrerios.cge.concursos.core.network

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Evita seguir golpeando al CGE mientras Wordfence mantiene el bloqueo activo.
 */
@Singleton
class CgeAccessGuard @Inject constructor() {

    @Volatile
    private var blockedUntilMs: Long = 0L

    fun isBlocked(): Boolean = System.currentTimeMillis() < blockedUntilMs

    fun remainingBlockMinutes(): Int {
        val ms = blockedUntilMs - System.currentTimeMillis()
        if (ms <= 0) return 0
        return ((ms + 59_999) / 60_000).toInt()
    }

    fun markBlocked(cooldownMinutes: Long = DEFAULT_COOLDOWN_MINUTES) {
        blockedUntilMs = System.currentTimeMillis() + cooldownMinutes * 60_000
    }

    fun clear() {
        blockedUntilMs = 0L
    }

    fun blockedMessage(): String {
        val mins = remainingBlockMinutes()
        return if (mins > 0) {
            "El CGE bloqueó el acceso. Reintentá en unos $mins min."
        } else {
            CgeAccessException.MSG_DEFAULT
        }
    }

    companion object {
        const val DEFAULT_COOLDOWN_MINUTES = 20L
    }
}
