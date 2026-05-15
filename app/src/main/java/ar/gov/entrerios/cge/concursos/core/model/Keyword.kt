package ar.gov.entrerios.cge.concursos.core.model

/**
 * Palabra clave del usuario. Se utiliza para filtrar concursos relevantes.
 */
data class Keyword(
    val id: Long = 0L,
    val text: String,
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
