package ar.gov.entrerios.cge.concursos.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import ar.gov.entrerios.cge.concursos.core.database.entity.ConcursoEntity
import ar.gov.entrerios.cge.concursos.core.database.entity.MatchEntity
import ar.gov.entrerios.cge.concursos.core.database.relation.ConcursoWithMatches
import kotlinx.coroutines.flow.Flow

@Dao
interface ConcursoDao {

    @Query("SELECT * FROM concursos WHERE url = :url LIMIT 1")
    suspend fun findByUrl(url: String): ConcursoEntity?

    @Query("SELECT id FROM concursos WHERE url = :url LIMIT 1")
    suspend fun idByUrl(url: String): Long?

    @Query("SELECT url FROM concursos")
    suspend fun allUrls(): List<String>

    @Transaction
    @Query("SELECT * FROM concursos ORDER BY (publishedAt IS NULL), publishedAt DESC, detectedAt DESC")
    fun observeAll(): Flow<List<ConcursoWithMatches>>

    @Transaction
    @Query(
        "SELECT * FROM concursos WHERE score > 0 " +
            "ORDER BY isNew DESC, score DESC, (publishedAt IS NULL), publishedAt DESC, detectedAt DESC"
    )
    fun observeRelevant(): Flow<List<ConcursoWithMatches>>

    @Transaction
    @Query("SELECT * FROM concursos WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<ConcursoWithMatches?>

    @Transaction
    @Query("SELECT * FROM concursos WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ConcursoWithMatches?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(concurso: ConcursoEntity): Long

    @Update
    suspend fun update(concurso: ConcursoEntity)

    @Query("UPDATE concursos SET isNew = 0 WHERE id = :id")
    suspend fun markAsSeen(id: Long)

    @Query("UPDATE concursos SET isRead = 1, isNew = 0 WHERE id = :id")
    suspend fun markAsRead(id: Long)

    @Query("UPDATE concursos SET isNew = 0")
    suspend fun clearAllNewFlags()

    @Query("DELETE FROM matches WHERE concursoId = :concursoId")
    suspend fun deleteMatchesFor(concursoId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatches(matches: List<MatchEntity>)

    @Transaction
    suspend fun replaceMatches(concursoId: Long, matches: List<MatchEntity>) {
        deleteMatchesFor(concursoId)
        if (matches.isNotEmpty()) insertMatches(matches)
    }

    @Query("DELETE FROM concursos WHERE detectedAt < :olderThan")
    suspend fun deleteOlderThan(olderThan: Long): Int
}
