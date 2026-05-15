package ar.gov.entrerios.cge.concursos.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "matches",
    foreignKeys = [
        ForeignKey(
            entity = ConcursoEntity::class,
            parentColumns = ["id"],
            childColumns = ["concursoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("concursoId"), Index(value = ["concursoId", "keyword", "location"], unique = true)]
)
data class MatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val concursoId: Long,
    val keyword: String,
    val location: String,
    val score: Int
)
