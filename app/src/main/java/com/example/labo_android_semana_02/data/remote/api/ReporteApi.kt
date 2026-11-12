package com.example.labo_android_semana_02.data.remote.api

import com.example.labo_android_semana_02.data.remote.dto.ReporteDto
import com.example.labo_android_semana_02.data.remote.dto.ReporteRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ReporteApi {
    @GET("api/reportes")
    suspend fun getReportes(
        @Header("Authorization") token: String
    ): Response<List<ReporteDto>>

    @POST("api/reportes")
    suspend fun createReporte(
        @Header("Authorization") token: String,
        @Body body: ReporteRequestDto
    ): Response<ReporteDto>

    @PUT("api/reportes/{id}")
    suspend fun updateReporte(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body body: ReporteRequestDto
    ): Response<ReporteDto>

    @DELETE("api/reportes/{id}")
    suspend fun deleteReporte(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<Unit>
}
