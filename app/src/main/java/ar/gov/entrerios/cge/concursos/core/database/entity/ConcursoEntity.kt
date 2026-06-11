package ar.gov.entrerios.cge.concursos.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "concursos",
    indices = [Index(value = ["url"], unique = true)]
)
data class ConcursoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val url: String,
    val title: String,
    val publishedAt: Long?,
    val categorySlug: String,
    val excerpt: String,
    val content: String,
    val contentHash: String,
    val detectedAt: Long,
    val isNew: Boolean,
    val isRead: Boolean,
    val score: Int,
    val deepScannedAt: Long? = null
)
