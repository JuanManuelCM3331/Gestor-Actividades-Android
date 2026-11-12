package com.example.labo_android_semana_02.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        CursoEntity::class,
        ActividadCursoEntity::class,
        MaterialEntity::class,
        PersonaCursoEntity::class,
    ],
    version = 6,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cursoDao(): CursoDao
    abstract fun actividadDao(): ActividadDao
    abstract fun materialDao(): MaterialDao
    abstract fun personaDao(): PersonaDao
}
