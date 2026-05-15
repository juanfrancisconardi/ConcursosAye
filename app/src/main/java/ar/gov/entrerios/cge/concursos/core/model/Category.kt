package ar.gov.entrerios.cge.concursos.core.model

/**
 * Categorías de concursos publicadas por el CGE Entre Ríos.
 * El slug corresponde al path de la página en `https://cge.entrerios.gov.ar/concursos/<slug>/`.
 */
enum class Category(
    val slug: String,
    val displayName: String
) {
    INICIAL("inicial", "Inicial"),
    PRIMARIO("primario", "Primario"),
    SECUNDARIO("secundario", "Secundario"),
    SUPERIOR("superior", "Superior"),
    SUPERVISOR("supervisor", "Supervisor"),
    GENERAL("", "General");

    companion object {
        fun fromSlug(slug: String?): Category =
            entries.firstOrNull { it.slug.equals(slug, ignoreCase = true) } ?: GENERAL

        /** Categorías que se monitorean (excluye GENERAL que es el listado raíz). */
        val monitored: List<Category> = listOf(INICIAL, PRIMARIO, SECUNDARIO, SUPERIOR, SUPERVISOR)
    }
}
