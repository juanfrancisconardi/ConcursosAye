package ar.gov.entrerios.cge.concursos.data.repository

import ar.gov.entrerios.cge.concursos.core.database.dao.SettingsDao
import ar.gov.entrerios.cge.concursos.core.model.AppSettings
import ar.gov.entrerios.cge.concursos.data.mapper.toDomain
import ar.gov.entrerios.cge.concursos.data.mapper.toEntity
import ar.gov.entrerios.cge.concursos.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dao: SettingsDao
) : SettingsRepository {

    override fun observe(): Flow<AppSettings> =
        dao.observe().map { it?.toDomain() ?: AppSettings() }

    override suspend fun get(): AppSettings = dao.get()?.toDomain() ?: AppSettings()

    override suspend fun update(settings: AppSettings) {
        dao.upsert(settings.toEntity())
    }
}
