package com.example.labo_android_semana_02

import com.example.labo_android_semana_02.domain.Prioridad
import com.example.labo_android_semana_02.domain.usecase.ValidarFormularioReporteUseCase
import com.example.labo_android_semana_02.ui.FormularioActividadUiState

/**
 * Función pura de validación expuesta para reutilización en ViewModels y pruebas unitarias.
 */
fun buildUiState(
    titulo: String,
    descripcion: String,
    fecha: String,
    prioridad: Prioridad,
    progreso: String,
    guardando: Boolean,
    evidenciaFotoUri: String? = null,
    evidenciaArchivoUri: String? = null,
    categoriaId: Int = 1
): FormularioActividadUiState {
    val validacion = ValidarFormularioReporteUseCase().invoke(
        titulo = titulo,
        descripcion = descripcion,
        fecha = fecha,
        prioridad = prioridad,
        progreso = progreso
    )
    return FormularioActividadUiState(
        titulo = titulo,
        descripcion = descripcion,
        fecha = fecha,
        prioridad = prioridad,
        progreso = progreso,
        categoriaId = categoriaId,
        errores = validacion.errores,
        puedeGuardar = validacion.puedeGuardar,
        guardando = guardando,
        evidenciaFotoUri = evidenciaFotoUri,
        evidenciaArchivoUri = evidenciaArchivoUri
    )
}
