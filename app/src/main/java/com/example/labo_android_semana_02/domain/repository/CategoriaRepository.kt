package com.example.labo_android_semana_02.domain.repository

import com.example.labo_android_semana_02.domain.Categoria
import kotlinx.coroutines.flow.Flow

interface CategoriaRepository {
    fun getCategorias(): Flow<List<Categoria>>
    suspend fun refreshCategorias(): Result<Unit>
}
