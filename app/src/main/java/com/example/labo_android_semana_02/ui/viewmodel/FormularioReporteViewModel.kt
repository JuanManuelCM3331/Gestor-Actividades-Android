package com.example.labo_android_semana_02.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.labo_android_semana_02.domain.Categoria
import com.example.labo_android_semana_02.domain.Prioridad
import com.example.labo_android_semana_02.domain.Reporte
import com.example.labo_android_semana_02.domain.repository.CategoriaRepository
import com.example.labo_android_semana_02.domain.repository.ReporteRepository
import com.example.labo_android_semana_02.domain.usecase.ValidarFormularioReporteUseCase
import com.example.labo_android_semana_02.ui.FormularioActividadUiState
import com.example.labo_android_semana_02.ui.OperacionUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class FormularioReporteViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val reporteRepository: ReporteRepository,
    private val categoriaRepository: CategoriaRepository,
    private val validarFormulario: ValidarFormularioReporteUseCase
) : ViewModel() {

    private val reporteId: Int? = savedStateHandle.get<Int>("reporteId")?.takeIf { it != 0 }

    private val _uiState = MutableStateFlow(
        validarYConstruir(
            FormularioActividadUiState(esEdicion = reporteId != null)
        )
    )
    val uiState: StateFlow<FormularioActividadUiState> = _uiState.asStateFlow()

    private val _operacionState = MutableStateFlow<OperacionUiState>(OperacionUiState.Ideal)
    val operacionState: StateFlow<OperacionUiState> = _operacionState.asStateFlow()

    init {
        observarCategorias()
        if (reporteId != null) {
            cargarReporteParaEdicion(reporteId)
        }
    }

    private fun observarCategorias() {
        viewModelScope.launch {
            categoriaRepository.getCategorias().collect { lista: List<Categoria> ->
                _uiState.update { actual ->
                    val categoriaValida = if (lista.any { it.id == actual.categoriaId }) {
                        actual.categoriaId
                    } else {
                        lista.firstOrNull()?.id ?: 1
                    }
                    actual.copy(categorias = lista, categoriaId = categoriaValida)
                }
            }
        }
    }

    private fun cargarReporteParaEdicion(id: Int) {
        viewModelScope.launch {
            val reporte = reporteRepository.getReporteById(id).firstOrNull() ?: return@launch
            val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val fechaStr = formato.format(Date(reporte.fecha))
            _uiState.update { actual ->
                validarYConstruir(
                    actual.copy(
                        titulo = reporte.titulo,
                        descripcion = reporte.descripcion,
                        fecha = fechaStr,
                        progreso = if (reporte.resuelto) "100" else "0",
                        categoriaId = reporte.categoriaId,
                        evidenciaFotoUri = reporte.evidenciaFotoUri,
                        evidenciaArchivoUri = reporte.evidenciaArchivoUri,
                        esEdicion = true
                    )
                )
            }
        }
    }

    fun onTituloChange(valor: String) = actualizarYValidar { it.copy(titulo = valor) }
    fun onDescripcionChange(valor: String) = actualizarYValidar { it.copy(descripcion = valor) }
    fun onFechaChange(valor: String) = actualizarYValidar { it.copy(fecha = valor) }
    fun onPrioridadChange(valor: Prioridad) = actualizarYValidar { it.copy(prioridad = valor) }
    fun onProgresoChange(valor: String) = actualizarYValidar { it.copy(progreso = valor) }
    fun onCategoriaChange(categoriaId: Int) = actualizarYValidar { it.copy(categoriaId = categoriaId) }
    fun onEvidenciaFotoChange(uri: String?) = actualizarYValidar { it.copy(evidenciaFotoUri = uri) }
    fun onEvidenciaArchivoChange(uri: String?) = actualizarYValidar { it.copy(evidenciaArchivoUri = uri) }

    fun limpiar() {
        _uiState.update { actual ->
            validarYConstruir(
                actual.copy(
                    titulo = "",
                    descripcion = "",
                    fecha = "",
                    progreso = "0",
                    evidenciaFotoUri = null,
                    evidenciaArchivoUri = null
                )
            )
        }
    }

    fun guardar() {
        val estadoActual = _uiState.value
        if (!estadoActual.puedeGuardar || estadoActual.guardando) return

        viewModelScope.launch {
            _uiState.update { it.copy(guardando = true) }
            _operacionState.value = OperacionUiState.Cargando

            try {
                val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val fechaLong = runCatching { format.parse(estadoActual.fecha)?.time }
                    .getOrNull() ?: System.currentTimeMillis()

                val reporte = Reporte(
                    id = reporteId ?: 0,
                    titulo = estadoActual.titulo.trim(),
                    descripcion = estadoActual.descripcion.trim(),
                    fecha = fechaLong,
                    categoriaId = estadoActual.categoriaId,
                    resuelto = estadoActual.progreso == "100",
                    evidenciaFotoUri = estadoActual.evidenciaFotoUri,
                    evidenciaArchivoUri = estadoActual.evidenciaArchivoUri
                )

                if (reporteId != null && reporteId > 0) {
                    reporteRepository.updateReporte(reporte)
                } else {
                    reporteRepository.insertReporte(reporte)
                }

                _uiState.update { it.copy(guardando = false) }
                _operacionState.value = OperacionUiState.Exito
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(guardando = false) }
                _operacionState.value = OperacionUiState.Error(e.message ?: "Error al guardar el reporte")
            }
        }
    }

    fun resetOperacion() {
        _operacionState.value = OperacionUiState.Ideal
    }

    private fun actualizarYValidar(transform: (FormularioActividadUiState) -> FormularioActividadUiState) {
        _uiState.update { actual -> validarYConstruir(transform(actual)) }
    }

    private fun validarYConstruir(estado: FormularioActividadUiState): FormularioActividadUiState {
        val validacion = validarFormulario(
            titulo = estado.titulo,
            descripcion = estado.descripcion,
            fecha = estado.fecha,
            prioridad = estado.prioridad,
            progreso = estado.progreso
        )
        return estado.copy(
            errores = validacion.errores,
            puedeGuardar = validacion.puedeGuardar
        )
    }
}
