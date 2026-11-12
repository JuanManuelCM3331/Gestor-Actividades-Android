package com.example.labo_android_semana_02.data.local.dao

import androidx.room.*
import com.example.labo_android_semana_02.data.local.entities.CategoriaEntity
import com.example.labo_android_semana_02.data.local.entities.ReporteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReporteDao {
    @Query("SELECT * FROM reportes WHERE pendienteEliminar = 0 ORDER BY fecha DESC")
    fun getAllReportes(): Flow<List<ReporteEntity>>

    @Query("SELECT * FROM reportes WHERE id = :id AND pendienteEliminar = 0")
    fun getReporteById(id: Int): Flow<ReporteEntity?>

    @Query("SELECT * FROM reportes WHERE categoriaId = :categoriaId AND pendienteEliminar = 0 ORDER BY fecha DESC")
    fun getReportesByCategoria(categoriaId: Int): Flow<List<ReporteEntity>>

    @Query("SELECT * FROM reportes WHERE sincronizado = 0 OR pendienteEliminar = 1")
    fun getPendientesSync(): List<ReporteEntity>

    @Query("SELECT MIN(id) FROM reportes")
    fun getMinId(): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertReporte(reporte: ReporteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertReportes(reportes: List<ReporteEntity>)

    @Update
    fun updateReporte(reporte: ReporteEntity)

    @Delete
    fun deleteReporte(reporte: ReporteEntity)

    @Query("DELETE FROM reportes WHERE id = :id")
    fun deleteReporteById(id: Int)

    @Query("DELETE FROM reportes WHERE sincronizado = 1 AND pendienteEliminar = 0")
    fun clearSyncedReportes()

    @Transaction
    fun replaceSyncedReportes(reportes: List<ReporteEntity>) {
        clearSyncedReportes()
        insertReportes(reportes)
    }

    // Categorías (conservado por compatibilidad con tests existentes)
    @Query("SELECT * FROM categorias ORDER BY id ASC")
    fun getAllCategorias(): Flow<List<CategoriaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCategoria(categoria: CategoriaEntity)
}
