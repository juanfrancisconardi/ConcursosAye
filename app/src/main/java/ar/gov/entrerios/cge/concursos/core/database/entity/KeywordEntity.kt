package ar.gov.entrerios.cge.concursos.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "keywords",
    indices = [Index(value = ["normalizedText"], unique = true)]
)
data class KeywordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val text: String,
    val normalizedText: String,
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
