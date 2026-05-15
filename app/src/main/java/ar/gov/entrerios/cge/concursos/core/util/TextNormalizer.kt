package ar.gov.entrerios.cge.concursos.core.util

import java.text.Normalizer
import java.util.Locale

/**
 * Normaliza texto para comparaciones tolerantes a acentos, mayúsculas y signos.
 *
 *   "Terapístà ocupAcional"  -> "terapista ocupacional"
 */
object TextNormalizer {

    private val accentRegex = "\\p{InCombiningDiacriticalMarks}+".toRegex()
    private val whitespaceRegex = "\\s+".toRegex()

    fun normalize(input: String): String {
        if (input.isBlank()) return ""
        val nfd = Normalizer.normalize(input, Normalizer.Form.NFD)
        val withoutAccents = accentRegex.replace(nfd, "")
        return withoutAccents.lowercase(Locale.ROOT)
            .replace(whitespaceRegex, " ")
            .trim()
    }
}
