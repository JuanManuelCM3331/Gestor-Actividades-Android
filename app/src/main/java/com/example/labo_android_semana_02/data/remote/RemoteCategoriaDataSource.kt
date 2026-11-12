package com.example.labo_android_semana_02.data.remote

import com.example.labo_android_semana_02.data.remote.api.CategoriaApi
import com.example.labo_android_semana_02.data.remote.dto.CategoriaDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteCategoriaDataSource @Inject constructor(
    private val api: CategoriaApi,
    private val tokenProvider: TokenProvider
) {
    suspend fun fetchCategorias(): Result<List<CategoriaDto>> =
        safeApiCall { api.getCategorias(tokenProvider.getToken()) }
}
