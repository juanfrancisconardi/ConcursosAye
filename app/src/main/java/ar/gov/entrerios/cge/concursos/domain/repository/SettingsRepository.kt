package ar.gov.entrerios.cge.concursos.domain.repository

import ar.gov.entrerios.cge.concursos.core.model.AppSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observe(): Flow<AppSettings>
    suspend fun get(): AppSettings
    suspend fun update(settings: AppSettings)
}
