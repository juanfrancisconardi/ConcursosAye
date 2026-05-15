package ar.gov.entrerios.cge.concursos.di

import android.content.Context
import androidx.room.Room
import ar.gov.entrerios.cge.concursos.core.database.AppDatabase
import ar.gov.entrerios.cge.concursos.core.database.dao.ConcursoDao
import ar.gov.entrerios.cge.concursos.core.database.dao.KeywordDao
import ar.gov.entrerios.cge.concursos.core.database.dao.SettingsDao
import ar.gov.entrerios.cge.concursos.core.util.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, Constants.DATABASE_NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideConcursoDao(db: AppDatabase): ConcursoDao = db.concursoDao()

    @Provides
    fun provideKeywordDao(db: AppDatabase): KeywordDao = db.keywordDao()

    @Provides
    fun provideSettingsDao(db: AppDatabase): SettingsDao = db.settingsDao()
}
