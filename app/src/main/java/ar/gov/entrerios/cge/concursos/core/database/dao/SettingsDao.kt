package ar.gov.entrerios.cge.concursos.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ar.gov.entrerios.cge.concursos.core.database.entity.SettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {

    @Query("SELECT * FROM configuraciones WHERE id = 1")
    fun observe(): Flow<SettingsEntity?>

    @Query("SELECT * FROM configuraciones WHERE id = 1")
    suspend fun get(): SettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: SettingsEntity)
}
