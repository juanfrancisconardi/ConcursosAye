package ar.gov.entrerios.cge.concursos.core.network

import java.io.IOException

/**
 * El sitio del CGE (Wordfence) rechazó la conexión por demasiadas consultas.
 * Extiende [IOException] para que OkHttp/Retrofit la propaguen sin tumbar el proceso.
 */
class CgeAccessException(
    message: String = MSG_DEFAULT
) : IOException(message) {
    companion object {
        const val MSG_DEFAULT =
            "El CGE limitó el acceso temporalmente (Wordfence). Esperá 15–30 minutos y no pulses actualizar varias veces seguidas."
    }
}

fun Throwable.findCgeAccessException(): CgeAccessException? {
    var current: Throwable? = this
    while (current != null) {
        if (current is CgeAccessException) return current
        current = current.cause
    }
    return null
}
