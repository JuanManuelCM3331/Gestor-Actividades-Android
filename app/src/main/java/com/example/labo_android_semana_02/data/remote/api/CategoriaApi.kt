package com.example.labo_android_semana_02.data.remote.api

import com.example.labo_android_semana_02.data.remote.dto.CategoriaDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header

interface CategoriaApi {
    @GET("api/categorias")
    suspend fun getCategorias(
        @Header("Authorization") token: String
    ): Response<List<CategoriaDto>>
}
