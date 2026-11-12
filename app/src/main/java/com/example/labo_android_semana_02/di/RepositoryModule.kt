package com.example.labo_android_semana_02.di

import com.example.labo_android_semana_02.data.repository.ActividadRepositoryImpl
import com.example.labo_android_semana_02.data.repository.AuthRepositoryImpl
import com.example.labo_android_semana_02.data.repository.CursoRepositoryImpl
import com.example.labo_android_semana_02.data.repository.ForoRepositoryImpl
import com.example.labo_android_semana_02.data.repository.MaterialRepositoryImpl
import com.example.labo_android_semana_02.data.repository.PreferenciasRepositoryImpl
import com.example.labo_android_semana_02.domain.repository.ActividadRepository
import com.example.labo_android_semana_02.domain.repository.AuthRepository
import com.example.labo_android_semana_02.domain.repository.CursoRepository
import com.example.labo_android_semana_02.domain.repository.ForoRepository
import com.example.labo_android_semana_02.domain.repository.MaterialRepository
import com.example.labo_android_semana_02.domain.repository.PreferenciasRepository
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
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindPreferenciasRepository(impl: PreferenciasRepositoryImpl): PreferenciasRepository

    @Binds
    @Singleton
    abstract fun bindCursoRepository(impl: CursoRepositoryImpl): CursoRepository

    @Binds
    @Singleton
    abstract fun bindActividadRepository(impl: ActividadRepositoryImpl): ActividadRepository

    @Binds
    @Singleton
    abstract fun bindMaterialRepository(impl: MaterialRepositoryImpl): MaterialRepository

    @Binds
    @Singleton
    abstract fun bindForoRepository(impl: ForoRepositoryImpl): ForoRepository
}
