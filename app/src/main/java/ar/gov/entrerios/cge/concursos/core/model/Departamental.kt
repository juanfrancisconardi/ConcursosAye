package ar.gov.entrerios.cge.concursos.core.model

/**
 * Direcciones Departamentales de Escuelas (DDE) del CGE Entre Ríos.
 *
 * [slug] es el path WordPress bajo el dominio del CGE (ej: `departamental-parana/`).
 * Cada página incluye un bloque de convocatorias con la misma estructura que `/concursos/`.
 */
enum class Departamental(val slug: String, val displayName: String) {
    NONE("", "Solo índice general (/concursos/)"),
    PARANA("departamental-parana", "Paraná"),
    COLON("departamental-colon", "Colón"),
    CONCORDIA("departamental-concordia", "Concordia"),
    DIAMANTE("departamental-diamante", "Diamante"),
    FEDERACION("departamental-federacion", "Federación"),
    FEDERAL("departamental-federal", "Federal"),
    FELICIANO("departamental-feliciano", "Feliciano"),
    GUALEGUAY("departamental-gualeguay", "Gualeguay"),
    GUALEGUAYCHU("departamental-gualeguaychu", "Gualeguaychú"),
    ISLAS_DEL_IBICUY("departamental-islas-del-ibicuy", "Islas del Ibicuy"),
    LA_PAZ("departamental-la-paz", "La Paz"),
    NOGOYA("departamental-nogoya", "Nogoyá"),
    SAN_SALVADOR("departamental-san-salvador", "San Salvador"),
    TALA("departamental-tala", "Tala"),
    URUGUAY("departamental-uruguay", "Uruguay"),
    VICTORIA("departamental-victoria", "Victoria"),
    VILLAGUAY("departamental-villaguay", "Villaguay");

    val isActive: Boolean get() = slug.isNotEmpty()

    companion object {
        fun fromSlug(slug: String?): Departamental {
            if (slug.isNullOrBlank()) return NONE
            return entries.firstOrNull { it.slug.equals(slug, ignoreCase = true) } ?: NONE
        }

        /** Opciones visibles en el desplegable de ajustes. */
        val selectable: List<Departamental> = entries.toList()
    }
}
