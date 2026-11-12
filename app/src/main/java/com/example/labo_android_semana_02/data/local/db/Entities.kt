package com.example.labo_android_semana_02.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cursos_cache")
data class CursoEntity(
    @PrimaryKey val id: Int,
    val ownerUserId: Int,
    val ownerRol: String,
    val instructorId: Int,
    val instructorNombre: String,
    val nombre: String,
    val descripcion: String,
    val estado: String,
    val codigoAcceso: String?,
    val codigoActivo: Boolean,
    val totalEstudiantes: Int,
    val totalActividades: Int,
    val nota05: Double?,
    val nota0100: Double?,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "actividades_cache")
data class ActividadCursoEntity(
    @PrimaryKey val id: Int,
    val cursoId: Int,
    val titulo: String,
    val descripcion: String,
    val fechaLimite: Long,
    val puntajeMaximo: Double,
    val adjuntosJson: String,
    val totalEntregas: Int,
    val porCalificar: Int,
    val estadoAprendiz: String?,
    val miEntregaJson: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "materiales_cache")
data class MaterialEntity(
    @PrimaryKey val id: Int,
    val cursoId: Int,
    val titulo: String,
    val descripcion: String,
    val tipo: String,
    val enlaceUrl: String?,
    val nombreOriginal: String?,
    val mimeType: String?,
    val tamanoBytes: Long?,
    val urlDescarga: String,
    val createdAt: Long,
)

@Entity(tableName = "personas_curso_cache", primaryKeys = ["cursoId", "id"])
data class PersonaCursoEntity(
    val id: Int,
    val cursoId: Int,
    val nombre: String,
    val email: String,
    val rol: String,
    val fechaUnion: Long?,
    val nota05: Double?,
    val nota0100: Double?,
)
