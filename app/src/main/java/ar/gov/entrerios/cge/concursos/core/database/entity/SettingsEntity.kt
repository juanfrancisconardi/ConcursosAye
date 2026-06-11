package ar.gov.entrerios.cge.concursos.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import ar.gov.entrerios.cge.concursos.core.model.Departamental

/**
 * Tabla con un único registro (id = 1) que guarda la configuración del usuario.
 */
@Entity(tableName = "configuraciones")
data class SettingsEntity(
    @PrimaryKey val id: Int = 1,
    /** "ON_APP_OPEN" | "DAILY" */
    val syncMode: String = "ON_APP_OPEN",
    val dailyHour: Int = 8,
    val dailyMinute: Int = 0,
    val notificationsEnabled: Boolean = true,
    val darkMode: String = "SYSTEM",
    /** Slugs de categorías separados por coma, ej: "inicial,primario,secundario". */
    val monitoredCategories: String = "inicial,primario,secundario,superior,supervisor",
    /** Ventana de días hacia atrás para sync y listados. */
    val syncDaysBack: Int = 30,
    /** Slug de la DDE seleccionada (vacío = solo /concursos/). */
    val departamentalSlug: String = Departamental.PARANA.slug
)
