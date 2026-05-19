package ar.gov.entrerios.cge.concursos.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE configuraciones ADD COLUMN syncDaysBack INTEGER NOT NULL DEFAULT 30"
            )
        }
    }
}
