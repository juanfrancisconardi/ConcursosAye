package ar.gov.entrerios.cge.concursos.di

import ar.gov.entrerios.cge.concursos.data.repository.ConcursoRepositoryImpl
import ar.gov.entrerios.cge.concursos.data.repository.KeywordRepositoryImpl
import ar.gov.entrerios.cge.concursos.data.repository.SettingsRepositoryImpl
import ar.gov.entrerios.cge.concursos.domain.repository.ConcursoRepository
import ar.gov.entrerios.cge.concursos.domain.repository.KeywordRepository
import ar.gov.entrerios.cge.concursos.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindConcursoRepository(impl: ConcursoRepositoryImpl): ConcursoRepository

    @Binds
    @Singleton
    abstract fun bindKeywordRepository(impl: KeywordRepositoryImpl): KeywordRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
