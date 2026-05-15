package ar.gov.entrerios.cge.concursos.core.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DateFormatter {

    private val displayFormat by lazy {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "AR")).apply {
            timeZone = TimeZone.getDefault()
        }
    }
    private val displayDateFormat by lazy {
        SimpleDateFormat("dd/MM/yyyy", Locale("es", "AR")).apply {
            timeZone = TimeZone.getDefault()
        }
    }

    /** Formatos posibles en la página del CGE. */
    private val parsers: List<SimpleDateFormat> = listOf(
        SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale("es", "AR")),
        SimpleDateFormat("dd MMMM, yyyy", Locale("es", "AR")),
        SimpleDateFormat("dd/MM/yyyy", Locale("es", "AR")),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US),
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US),
        SimpleDateFormat("yyyy-MM-dd", Locale.US)
    )

    fun parsePublishedDate(raw: String?): Long? {
        val clean = raw?.trim().orEmpty()
        if (clean.isEmpty()) return null
        for (parser in parsers) {
            try {
                return parser.parse(clean)?.time
            } catch (_: Throwable) { /* siguiente parser */ }
        }
        return null
    }

    fun formatDate(timestamp: Long?): String =
        timestamp?.let { displayDateFormat.format(Date(it)) } ?: "—"

    fun formatDateTime(timestamp: Long?): String =
        timestamp?.let { displayFormat.format(Date(it)) } ?: "—"
}
