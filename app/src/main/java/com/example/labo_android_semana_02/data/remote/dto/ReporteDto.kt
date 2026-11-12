package com.example.labo_android_semana_02.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReporteDto(
    val id: Int,
    val titulo: String,
    val descripcion: String = "",
    val fecha: Long,
    @SerialName("categoria_id")
    val categoriaId: Int,
    @SerialName("categoria_nombre")
    val categoriaNombre: String? = null,
    @SerialName("categoria_icono")
    val categoriaIcono: String? = null,
    val resuelto: Boolean = false,
    @SerialName("evidencia_foto_uri")
    val evidenciaFotoUri: String? = null,
    @SerialName("evidencia_archivo_uri")
    val evidenciaArchivoUri: String? = null
)

@Serializable
data class ReporteRequestDto(
    val titulo: String,
    val descripcion: String,
    val fecha: Long,
    @SerialName("categoria_id")
    val categoriaId: Int,
    val resuelto: Boolean,
    @SerialName("evidencia_foto_uri")
    val evidenciaFotoUri: String? = null,
    @SerialName("evidencia_archivo_uri")
    val evidenciaArchivoUri: String? = null
)
