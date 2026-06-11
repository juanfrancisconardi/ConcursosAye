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

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE concursos ADD COLUMN deepScannedAt INTEGER"
            )
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE configuraciones ADD COLUMN departamentalSlug TEXT NOT NULL DEFAULT ''"
            )
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                UPDATE configuraciones
                SET departamentalSlug = 'departamental-parana'
                WHERE departamentalSlug = '' OR departamentalSlug IS NULL
                """.trimIndent()
            )
        }
    }
}
