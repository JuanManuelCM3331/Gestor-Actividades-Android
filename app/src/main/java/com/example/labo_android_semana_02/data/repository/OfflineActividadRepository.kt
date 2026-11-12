package com.example.labo_android_semana_02.data.repository

import com.example.labo_android_semana_02.data.local.dao.ActividadDao
import com.example.labo_android_semana_02.data.mapper.toDomain
import com.example.labo_android_semana_02.data.mapper.toEntity
import com.example.labo_android_semana_02.data.remote.RemoteActividadDataSource
import com.example.labo_android_semana_02.domain.ActividadFormativa
import com.example.labo_android_semana_02.domain.repository.ActividadRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineActividadRepository @Inject constructor(
    private val actividadDao: ActividadDao,
    private val remoteDataSource: RemoteActividadDataSource
) : ActividadRepository {

    override fun getActividades(): Flow<List<ActividadFormativa>> {
        return actividadDao.getAllActividades().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getActividadById(id: Int): Flow<ActividadFormativa?> {
        return actividadDao.getActividadById(id).map { it?.toDomain() }
    }

    override suspend fun refresh(): Result<Unit> = withContext(Dispatchers.IO) {
        val result = remoteDataSource.fetchActividades()
        if (result.isSuccess) {
            val dtos = result.getOrNull() ?: emptyList()
            actividadDao.refreshActividades(dtos.map { it.toEntity() })
            Result.success(Unit)
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Error desconocido"))
        }
    }
}
