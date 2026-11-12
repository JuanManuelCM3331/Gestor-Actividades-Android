package com.example.labo_android_semana_02.di

import com.example.labo_android_semana_02.data.local.datastore.DataStorePreferenciasRepository
import com.example.labo_android_semana_02.data.repository.OfflineActividadRepository
import com.example.labo_android_semana_02.data.repository.OfflineFirstCategoriaRepository
import com.example.labo_android_semana_02.data.repository.OfflineFirstReporteRepository
import com.example.labo_android_semana_02.domain.repository.ActividadRepository
import com.example.labo_android_semana_02.domain.repository.CategoriaRepository
import com.example.labo_android_semana_02.domain.repository.PreferenciasRepository
import com.example.labo_android_semana_02.domain.repository.ReporteRepository
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
    abstract fun bindReporteRepository(
        impl: OfflineFirstReporteRepository
    ): ReporteRepository

    @Binds
    @Singleton
    abstract fun bindCategoriaRepository(
        impl: OfflineFirstCategoriaRepository
    ): CategoriaRepository

    @Binds
    @Singleton
    abstract fun bindActividadRepository(
        impl: OfflineActividadRepository
    ): ActividadRepository

    @Binds
    @Singleton
    abstract fun bindPreferenciasRepository(
        impl: DataStorePreferenciasRepository
    ): PreferenciasRepository
}
