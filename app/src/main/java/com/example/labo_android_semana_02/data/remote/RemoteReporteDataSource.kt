package com.example.labo_android_semana_02.data.remote

import com.example.labo_android_semana_02.data.remote.api.ReporteApi
import com.example.labo_android_semana_02.data.remote.dto.ReporteDto
import com.example.labo_android_semana_02.data.remote.dto.ReporteRequestDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteReporteDataSource @Inject constructor(
    private val api: ReporteApi,
    private val tokenProvider: TokenProvider
) {
    suspend fun fetchReportes(): Result<List<ReporteDto>> =
        safeApiCall { api.getReportes(tokenProvider.getToken()) }

    suspend fun createReporte(request: ReporteRequestDto): Result<ReporteDto> =
        safeApiCall { api.createReporte(tokenProvider.getToken(), request) }

    suspend fun updateReporte(id: Int, request: ReporteRequestDto): Result<ReporteDto> =
        safeApiCall { api.updateReporte(tokenProvider.getToken(), id, request) }

    suspend fun deleteReporte(id: Int): Result<Unit> =
        safeApiCall { api.deleteReporte(tokenProvider.getToken(), id) }
}
