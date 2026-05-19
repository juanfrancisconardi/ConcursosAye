package ar.gov.entrerios.cge.concursos.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import ar.gov.entrerios.cge.concursos.core.database.entity.KeywordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface KeywordDao {

    @Query("SELECT * FROM keywords ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<KeywordEntity>>

    @Query("SELECT * FROM keywords WHERE enabled = 1")
    suspend fun getActive(): List<KeywordEntity>

    @Query("SELECT * FROM keywords WHERE normalizedText = :normalized LIMIT 1")
    suspend fun findByNormalized(normalized: String): KeywordEntity?

    @Query("SELECT * FROM keywords")
    suspend fun getAll(): List<KeywordEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(keyword: KeywordEntity): Long

    @Update
    suspend fun update(keyword: KeywordEntity)

    @Query("DELETE FROM keywords WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE keywords SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)
}
