package com.example.labo_android_semana_02.di

import android.content.Context
import androidx.room.Room
import com.example.labo_android_semana_02.data.local.dao.ActividadDao
import com.example.labo_android_semana_02.data.local.dao.CategoriaDao
import com.example.labo_android_semana_02.data.local.dao.ReporteDao
import com.example.labo_android_semana_02.data.local.db.AppDatabase
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
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "reportes_db")
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5
            )
            .addCallback(AppDatabase.SEED_CALLBACK)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideReporteDao(db: AppDatabase): ReporteDao = db.reporteDao()

    @Provides
    fun provideCategoriaDao(db: AppDatabase): CategoriaDao = db.categoriaDao()

    @Provides
    fun provideActividadDao(db: AppDatabase): ActividadDao = db.actividadDao()
}
