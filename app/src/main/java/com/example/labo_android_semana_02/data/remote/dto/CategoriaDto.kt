package com.example.labo_android_semana_02.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CategoriaDto(
    val id: Int,
    val nombre: String,
    val icono: String
)
