package com.example.labo_android_semana_02.data.remote

import com.example.labo_android_semana_02.data.remote.api.ActividadApi
import com.example.labo_android_semana_02.data.remote.dto.ActividadDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteActividadDataSource @Inject constructor(
    private val api: ActividadApi,
    private val tokenProvider: TokenProvider
) {
    suspend fun fetchActividades(): Result<List<ActividadDto>> =
        safeApiCall { api.getActividades(tokenProvider.getToken()) }
}
