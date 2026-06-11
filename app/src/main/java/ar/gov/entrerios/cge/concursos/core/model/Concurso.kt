package ar.gov.entrerios.cge.concursos.core.model

/**
 * Modelo de dominio de un concurso publicado por el CGE.
 */
data class Concurso(
    val id: Long = 0L,
    val url: String,
    val title: String,
    val publishedAt: Long?,
    val category: Category,
    val excerpt: String,
    val content: String,
    val contentHash: String = "",
    val detectedAt: Long,
    val isNew: Boolean,
    val isRead: Boolean,
    val matches: List<KeywordMatch> = emptyList(),
    val score: Int = 0,
    val deepScannedAt: Long? = null
)

/**
 * Indica dónde se encontró la coincidencia con la keyword.
 */
enum class MatchLocation { TITLE, CONTENT }

data class KeywordMatch(
    val keyword: String,
    val location: MatchLocation,
    val score: Int
)
