package ar.gov.entrerios.cge.concursos.core.model

import ar.gov.entrerios.cge.concursos.core.util.ConcursoDateFilter

/**
 * Configuración de la aplicación, controlada por el usuario.
 */
data class AppSettings(
    val syncMode: SyncMode = SyncMode.ON_APP_OPEN,
    /** Solo sincronizar y mostrar avisos de los últimos N días (por fecha de publicación). */
    val syncDaysBack: Int = ConcursoDateFilter.DEFAULT_DAYS_BACK,
    /** Hora del día (0-23) en la que se hace el chequeo automático cuando [syncMode] = DAILY. */
    val dailyHour: Int = 8,
    /** Minuto del día (0-59) en la que se hace el chequeo automático cuando [syncMode] = DAILY. */
    val dailyMinute: Int = 0,
    val notificationsEnabled: Boolean = true,
    val darkMode: DarkMode = DarkMode.SYSTEM,
    val monitoredCategories: Set<Category> = Category.monitored.toSet()
)

/**
 * Modo de actualización elegido por el usuario:
 *
 * - [ON_APP_OPEN]: el chequeo se ejecuta solo cuando el usuario abre la aplicación.
 * - [DAILY]: el chequeo se programa todos los días a la hora indicada por
 *   [AppSettings.dailyHour] / [AppSettings.dailyMinute] (vía WorkManager).
 */
enum class SyncMode { ON_APP_OPEN, DAILY }

enum class DarkMode { LIGHT, DARK, SYSTEM }
