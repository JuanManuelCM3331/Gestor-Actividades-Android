package com.example.labo_android_semana_02.data.repository

import com.example.labo_android_semana_02.data.local.dao.CategoriaDao
import com.example.labo_android_semana_02.data.local.entities.CategoriaEntity
import com.example.labo_android_semana_02.data.mapper.toDomain
import com.example.labo_android_semana_02.data.mapper.toEntity
import com.example.labo_android_semana_02.data.remote.RemoteCategoriaDataSource
import com.example.labo_android_semana_02.domain.Categoria
import com.example.labo_android_semana_02.domain.repository.CategoriaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineFirstCategoriaRepository @Inject constructor(
    private val categoriaDao: CategoriaDao,
    private val remoteDataSource: RemoteCategoriaDataSource
) : CategoriaRepository {

    override fun getCategorias(): Flow<List<Categoria>> {
        return categoriaDao.getAllCategorias().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun refreshCategorias(): Result<Unit> = withContext(Dispatchers.IO) {
        ensureDefaultCategoriasLocal()
        val result = remoteDataSource.fetchCategorias()
        result.fold(
            onSuccess = { dtos ->
                if (dtos.isNotEmpty()) {
                    categoriaDao.replaceCategorias(dtos.map { it.toEntity() })
                }
                Result.success(Unit)
            },
            onFailure = { err ->
                Result.failure(err)
            }
        )
    }

    internal fun ensureDefaultCategoriasLocal() {
        if (categoriaDao.getCount() == 0) {
            categoriaDao.insertCategorias(
                listOf(
                    CategoriaEntity(1, "General", "📋"),
                    CategoriaEntity(2, "Infraestructura", "🏗️"),
                    CategoriaEntity(3, "Académico", "📚"),
                    CategoriaEntity(4, "Soporte TI", "💻")
                )
            )
        }
    }
}
