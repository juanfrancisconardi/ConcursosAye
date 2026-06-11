package ar.gov.entrerios.cge.concursos.core.util

import ar.gov.entrerios.cge.concursos.core.model.Keyword
import ar.gov.entrerios.cge.concursos.core.model.KeywordMatch
import ar.gov.entrerios.cge.concursos.core.model.MatchLocation

/**
 * Reglas de scoring:
 *   - Coincidencia en TÍTULO    => 10 puntos por keyword
 *   - Coincidencia en CONTENIDO => 4 puntos por keyword
 *
 * El matching:
 *   - ignora mayúsculas/minúsculas (TextNormalizer.lowercase)
 *   - tolera acentos (TextNormalizer.NFD + remove diacritics)
 *   - para keywords cortas (<= 4 chars, sin espacios) exige palabra completa
 *   - para frases o keywords largas permite coincidencia parcial
 */
object KeywordMatcher {

    private const val TITLE_SCORE = 10
    private const val CONTENT_SCORE = 4
    private const val SHORT_KEYWORD_MAX_LENGTH = 4
    private val boundaryCharClass = "[^\\p{L}\\p{Nd}]"

    /**
     * Calcula las coincidencias de [keywords] dentro de [title] y [content].
     * Solo considera keywords con `enabled = true`.
     */
    fun match(title: String, content: String, keywords: List<Keyword>): List<KeywordMatch> {
        val active = keywords.filter { it.enabled && it.text.isNotBlank() }
        if (active.isEmpty()) return emptyList()

        val normalizedTitle = TextNormalizer.normalize(title)
        val normalizedContent = TextNormalizer.normalize(content)

        val matches = mutableListOf<KeywordMatch>()
        for (keyword in active) {
            val normalizedKeyword = TextNormalizer.normalize(keyword.text)
            if (normalizedKeyword.isEmpty()) continue

            val inTitle = containsKeyword(normalizedTitle, normalizedKeyword)
            val inContent = containsKeyword(normalizedContent, normalizedKeyword)

            when {
                inTitle -> matches.add(
                    KeywordMatch(
                        keyword = keyword.text,
                        location = MatchLocation.TITLE,
                        score = TITLE_SCORE
                    )
                )
                inContent -> matches.add(
                    KeywordMatch(
                        keyword = keyword.text,
                        location = MatchLocation.CONTENT,
                        score = CONTENT_SCORE
                    )
                )
            }
        }
        return matches
    }

    private fun containsKeyword(normalizedText: String, normalizedKeyword: String): Boolean {
        if (normalizedText.isEmpty() || normalizedKeyword.isEmpty()) return false

        val isShortSingleWord =
            normalizedKeyword.length <= SHORT_KEYWORD_MAX_LENGTH &&
                !normalizedKeyword.contains(' ')

        if (!isShortSingleWord) {
            return normalizedText.contains(normalizedKeyword)
        }

        val escaped = Regex.escape(normalizedKeyword)
        val pattern = "(^|$boundaryCharClass)$escaped($boundaryCharClass|$)".toRegex()
        return pattern.containsMatchIn(normalizedText)
    }

    fun totalScore(matches: List<KeywordMatch>): Int = matches.sumOf { it.score }
}
