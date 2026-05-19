package ar.gov.entrerios.cge.concursos.core.model

/**
 * Categorías de concursos publicadas por el CGE Entre Ríos.
 *
 * [slug] se persiste en ajustes del usuario.
 * [archiveSlug] es el slug de la categoría WordPress (`/category/<archiveSlug>/`).
 * Los avisos de concurso en sí viven en permalinks `/YYYY/MM/titulo/`, no bajo `/concursos/`.
 */
enum class Category(
    val slug: String,
    val archiveSlug: String,
    val displayName: String
) {
    INICIAL("inicial", "concursos-inicial", "Inicial"),
    PRIMARIO("primario", "concursos-nivel-primario", "Primario"),
    SECUNDARIO("secundario", "concursos-nivel-secundario", "Secundario"),
    SUPERIOR("superior", "concursos-nivel-superior", "Superior"),
    SUPERVISOR("supervisor", "concursos-nivel-supervisor", "Supervisor"),
    GENERAL("", "", "General");

    companion object {
        fun fromSlug(slug: String?): Category =
            entries.firstOrNull { it.slug.equals(slug, ignoreCase = true) } ?: GENERAL

        /**
         * Las cinco fuentes oficiales de concursos en el CGE (una por nivel).
         * La sync recorre **todas** las seleccionadas en ajustes (por defecto, las cinco).
         */
        val monitored: List<Category> = listOf(INICIAL, PRIMARIO, SECUNDARIO, SUPERIOR, SUPERVISOR)
    }
}
