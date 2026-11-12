package com.example.labo_android_semana_02.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface PreferenciasRepository {
    val ordenFiltro: Flow<String>
    val temaOscuro: Flow<Boolean?>
        get() = flowOf(null)
    suspend fun guardarOrdenFiltro(orden: String)
    suspend fun guardarTemaOscuro(oscuro: Boolean) {}
}
