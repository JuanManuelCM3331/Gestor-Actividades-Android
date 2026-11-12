package com.example.labo_android_semana_02.di

import android.content.Context
import androidx.room.Room
import com.example.labo_android_semana_02.data.local.db.ActividadDao
import com.example.labo_android_semana_02.data.local.db.AppDatabase
import com.example.labo_android_semana_02.data.local.db.CursoDao
import com.example.labo_android_semana_02.data.local.db.MaterialDao
import com.example.labo_android_semana_02.data.local.db.PersonaDao
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
        Room.databaseBuilder(context, AppDatabase::class.java, "ctma_lms_cache.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideCursoDao(db: AppDatabase): CursoDao = db.cursoDao()

    @Provides
    fun provideActividadDao(db: AppDatabase): ActividadDao = db.actividadDao()

    @Provides
    fun provideMaterialDao(db: AppDatabase): MaterialDao = db.materialDao()

    @Provides
    fun providePersonaDao(db: AppDatabase): PersonaDao = db.personaDao()
}
