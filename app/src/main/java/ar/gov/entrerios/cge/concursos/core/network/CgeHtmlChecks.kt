package ar.gov.entrerios.cge.concursos.core.network

/**
 * Detecta respuestas de bloqueo Wordfence del CGE (suelen venir como HTTP 503).
 */
object CgeHtmlChecks {

  private val BLOCK_MARKERS = listOf(
        "wordfence",
        "ha sido limitado",
        "has been limited",
        "maximum global requests per minute",
        "motivo del bloqueo"
    )

    fun isAccessBlocked(html: String): Boolean {
        val lower = html.lowercase()
        return BLOCK_MARKERS.any { lower.contains(it) }
    }

    fun ensureNotBlocked(html: String) {
        if (isAccessBlocked(html)) {
            throw CgeAccessException()
        }
    }
}
