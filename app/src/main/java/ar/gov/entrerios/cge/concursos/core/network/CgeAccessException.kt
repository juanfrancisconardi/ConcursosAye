package ar.gov.entrerios.cge.concursos.core.network

/**
 * El sitio del CGE (Wordfence) rechazó la conexión por demasiadas consultas.
 */
class CgeAccessException(
    message: String = MSG_DEFAULT
) : Exception(message) {
    companion object {
        const val MSG_DEFAULT =
            "El CGE limitó el acceso temporalmente (Wordfence). Esperá 15–30 minutos y no pulses actualizar varias veces seguidas."
    }
}
