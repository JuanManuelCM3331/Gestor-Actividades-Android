package com.example.labo_android_semana_02.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.labo_android_semana_02.data.local.entities.CategoriaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoriaDao {
    @Query("SELECT * FROM categorias ORDER BY id ASC")
    fun getAllCategorias(): Flow<List<CategoriaEntity>>

    @Query("SELECT COUNT(*) FROM categorias")
    fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCategorias(categorias: List<CategoriaEntity>)

    @Transaction
    fun replaceCategorias(categorias: List<CategoriaEntity>) {
        insertCategorias(categorias)
    }
}
