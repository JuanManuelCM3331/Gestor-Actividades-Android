package com.example.labo_android_semana_02.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface CursoDao {
    @Query("SELECT * FROM cursos_cache WHERE ownerUserId = :userId AND ownerRol = :rol ORDER BY updatedAt DESC")
    fun observarPorUsuario(userId: Int, rol: String): Flow<List<CursoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertarTodos(cursos: List<CursoEntity>)

    @Query("DELETE FROM cursos_cache WHERE ownerUserId = :userId AND ownerRol = :rol")
    fun eliminarPorUsuario(userId: Int, rol: String)

    @Query("DELETE FROM cursos_cache")
    fun eliminarTodo()

    @Transaction
    fun reemplazarPorUsuario(userId: Int, rol: String, cursos: List<CursoEntity>) {
        eliminarPorUsuario(userId, rol)
        if (cursos.isNotEmpty()) insertarTodos(cursos)
    }
}

@Dao
interface ActividadDao {
    @Query("SELECT * FROM actividades_cache WHERE cursoId = :cursoId ORDER BY fechaLimite ASC")
    fun observarPorCurso(cursoId: Int): Flow<List<ActividadCursoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertarTodas(actividades: List<ActividadCursoEntity>)

    @Query("DELETE FROM actividades_cache WHERE cursoId = :cursoId")
    fun eliminarPorCurso(cursoId: Int)

    @Query("DELETE FROM actividades_cache")
    fun eliminarTodo()

    @Transaction
    fun reemplazarPorCurso(cursoId: Int, actividades: List<ActividadCursoEntity>) {
        eliminarPorCurso(cursoId)
        if (actividades.isNotEmpty()) insertarTodas(actividades)
    }
}

@Dao
interface MaterialDao {
    @Query("SELECT * FROM materiales_cache WHERE cursoId = :cursoId ORDER BY createdAt DESC")
    fun observarPorCurso(cursoId: Int): Flow<List<MaterialEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertarTodos(materiales: List<MaterialEntity>)

    @Query("DELETE FROM materiales_cache WHERE cursoId = :cursoId")
    fun eliminarPorCurso(cursoId: Int)

    @Query("DELETE FROM materiales_cache")
    fun eliminarTodo()

    @Transaction
    fun reemplazarPorCurso(cursoId: Int, materiales: List<MaterialEntity>) {
        eliminarPorCurso(cursoId)
        if (materiales.isNotEmpty()) insertarTodos(materiales)
    }
}

@Dao
interface PersonaDao {
    @Query("SELECT * FROM personas_curso_cache WHERE cursoId = :cursoId ORDER BY rol DESC, nombre ASC")
    fun observarPorCurso(cursoId: Int): Flow<List<PersonaCursoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertarTodas(personas: List<PersonaCursoEntity>)

    @Query("DELETE FROM personas_curso_cache WHERE cursoId = :cursoId")
    fun eliminarPorCurso(cursoId: Int)

    @Query("DELETE FROM personas_curso_cache")
    fun eliminarTodo()

    @Transaction
    fun reemplazarPorCurso(cursoId: Int, personas: List<PersonaCursoEntity>) {
        eliminarPorCurso(cursoId)
        if (personas.isNotEmpty()) insertarTodas(personas)
    }
}
