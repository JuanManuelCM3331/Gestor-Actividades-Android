package com.example.labo_android_semana_02.data.repository

import com.example.labo_android_semana_02.data.local.dao.ReporteDao
import com.example.labo_android_semana_02.data.local.entities.ReporteEntity
import com.example.labo_android_semana_02.data.mapper.toDomain
import com.example.labo_android_semana_02.data.mapper.toEntity
import com.example.labo_android_semana_02.data.mapper.toRequestDto
import com.example.labo_android_semana_02.data.remote.HttpStatusException
import com.example.labo_android_semana_02.data.remote.RemoteReporteDataSource
import com.example.labo_android_semana_02.domain.Reporte
import com.example.labo_android_semana_02.domain.repository.ReporteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineFirstReporteRepository @Inject constructor(
    private val reporteDao: ReporteDao,
    private val categoriaRepository: OfflineFirstCategoriaRepository,
    private val remoteDataSource: RemoteReporteDataSource
) : ReporteRepository {

    override fun getReportes(): Flow<List<Reporte>> = reporteDao.getAllReportes().map { list ->
        list.map { it.toDomain() }
    }

    override fun getReporteById(id: Int): Flow<Reporte?> = reporteDao.getReporteById(id).map {
        it?.toDomain()
    }

    override suspend fun refreshReportes(): Result<Unit> = withContext(Dispatchers.IO) {
        categoriaRepository.refreshCategorias()
        pushPendientes()

        val result = remoteDataSource.fetchReportes()
        result.fold(
            onSuccess = { dtos ->
                reporteDao.replaceSyncedReportes(dtos.map { it.toEntity() })
                Result.success(Unit)
            },
            onFailure = { err ->
                Result.failure(err)
            }
        )
    }

    override suspend fun insertReporte(reporte: Reporte) = withContext(Dispatchers.IO) {
        categoriaRepository.ensureDefaultCategoriasLocal()
        if (reporte.id > 0) {
            updateReporte(reporte)
            return@withContext
        }

        val remoteResult = remoteDataSource.createReporte(reporte.toRequestDto())
        remoteResult.fold(
            onSuccess = { createdDto ->
                if (reporte.id < 0) {
                    reporteDao.deleteReporteById(reporte.id)
                }
                reporteDao.insertReporte(createdDto.toEntity())
            },
            onFailure = {
                val localTempId = if (reporte.id < 0) {
                    reporte.id
                } else {
                    val minId = reporteDao.getMinId() ?: 0
                    minOf(0, minId) - 1
                }
                reporteDao.insertReporte(
                    reporte.copy(id = localTempId).toEntity(sincronizado = false)
                )
            }
        )
    }

    override suspend fun updateReporte(reporte: Reporte) = withContext(Dispatchers.IO) {
        categoriaRepository.ensureDefaultCategoriasLocal()
        if (reporte.id <= 0) {
            reporteDao.insertReporte(reporte.toEntity(sincronizado = false))
            return@withContext
        }

        val remoteResult = remoteDataSource.updateReporte(reporte.id, reporte.toRequestDto())
        remoteResult.fold(
            onSuccess = { updatedDto ->
                reporteDao.insertReporte(updatedDto.toEntity())
            },
            onFailure = {
                reporteDao.insertReporte(reporte.toEntity(sincronizado = false))
            }
        )
    }

    override suspend fun deleteReporte(reporte: Reporte) = withContext(Dispatchers.IO) {
        if (reporte.id <= 0) {
            reporteDao.deleteReporteById(reporte.id)
            return@withContext
        }

        // Ocultamos inmediatamente en caché local para respuesta instantánea en UI
        reporteDao.insertReporte(reporte.toEntity(sincronizado = false, pendienteEliminar = true))

        val remoteResult = remoteDataSource.deleteReporte(reporte.id)
        remoteResult.fold(
            onSuccess = {
                reporteDao.deleteReporteById(reporte.id)
            },
            onFailure = { err ->
                if (err is HttpStatusException && err.code == 404) {
                    reporteDao.deleteReporteById(reporte.id)
                }
            }
        )
    }

    private suspend fun pushPendientes() {
        val pendientes: List<ReporteEntity> = reporteDao.getPendientesSync()
        for (entity in pendientes) {
            if (entity.pendienteEliminar) {
                if (entity.id <= 0) {
                    reporteDao.deleteReporteById(entity.id)
                } else {
                    val delRes = remoteDataSource.deleteReporte(entity.id)
                    val err = delRes.exceptionOrNull()
                    if (delRes.isSuccess || (err is HttpStatusException && err.code == 404)) {
                        reporteDao.deleteReporteById(entity.id)
                    }
                }
            } else if (entity.id <= 0) {
                val createRes = remoteDataSource.createReporte(entity.toRequestDto())
                createRes.onSuccess { created ->
                    reporteDao.deleteReporteById(entity.id)
                    reporteDao.insertReporte(created.toEntity())
                }
            } else {
                val updateRes = remoteDataSource.updateReporte(entity.id, entity.toRequestDto())
                updateRes.onSuccess { updated ->
                    reporteDao.insertReporte(updated.toEntity())
                }
            }
        }
    }
}
