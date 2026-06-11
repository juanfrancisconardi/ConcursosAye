package ar.gov.entrerios.cge.concursos.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import ar.gov.entrerios.cge.concursos.core.database.dao.ConcursoDao
import ar.gov.entrerios.cge.concursos.core.database.dao.KeywordDao
import ar.gov.entrerios.cge.concursos.core.database.dao.SettingsDao
import ar.gov.entrerios.cge.concursos.core.database.entity.ConcursoEntity
import ar.gov.entrerios.cge.concursos.core.database.entity.KeywordEntity
import ar.gov.entrerios.cge.concursos.core.database.entity.MatchEntity
import ar.gov.entrerios.cge.concursos.core.database.entity.SettingsEntity

@Database(
    entities = [
        ConcursoEntity::class,
        KeywordEntity::class,
        MatchEntity::class,
        SettingsEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun concursoDao(): ConcursoDao
    abstract fun keywordDao(): KeywordDao
    abstract fun settingsDao(): SettingsDao
}
