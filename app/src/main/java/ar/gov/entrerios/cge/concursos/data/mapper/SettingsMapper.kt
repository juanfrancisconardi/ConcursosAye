package ar.gov.entrerios.cge.concursos.data.mapper

import ar.gov.entrerios.cge.concursos.core.database.entity.SettingsEntity
import ar.gov.entrerios.cge.concursos.core.model.AppSettings
import ar.gov.entrerios.cge.concursos.core.model.Category
import ar.gov.entrerios.cge.concursos.core.model.DarkMode
import ar.gov.entrerios.cge.concursos.core.model.SyncMode

fun SettingsEntity.toDomain(): AppSettings = AppSettings(
    syncMode = runCatching { SyncMode.valueOf(syncMode) }.getOrDefault(SyncMode.ON_APP_OPEN),
    dailyHour = dailyHour.coerceIn(0, 23),
    dailyMinute = dailyMinute.coerceIn(0, 59),
    notificationsEnabled = notificationsEnabled,
    darkMode = runCatching { DarkMode.valueOf(darkMode) }.getOrDefault(DarkMode.SYSTEM),
    monitoredCategories = monitoredCategories
        .split(',')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .map { Category.fromSlug(it) }
        .toSet()
        .ifEmpty { Category.monitored.toSet() }
)

fun AppSettings.toEntity(): SettingsEntity = SettingsEntity(
    id = 1,
    syncMode = syncMode.name,
    dailyHour = dailyHour.coerceIn(0, 23),
    dailyMinute = dailyMinute.coerceIn(0, 59),
    notificationsEnabled = notificationsEnabled,
    darkMode = darkMode.name,
    monitoredCategories = monitoredCategories.joinToString(",") { it.slug }
)
