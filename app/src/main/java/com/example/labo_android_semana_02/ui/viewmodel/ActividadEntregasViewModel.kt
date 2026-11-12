package com.example.labo_android_semana_02.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.labo_android_semana_02.data.remote.FileDownloadManager
import com.example.labo_android_semana_02.domain.model.Actividad
import com.example.labo_android_semana_02.domain.model.ArchivoAdjunto
import com.example.labo_android_semana_02.domain.model.EntregaInstructorItem
import com.example.labo_android_semana_02.domain.repository.ActividadRepository
import com.example.labo_android_semana_02.domain.usecase.ValidarFormulariosUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ActividadEntregasUiState(
    val actividad: Actividad? = null,
    val entregas: List<EntregaInstructorItem> = emptyList(),
    val cargando: Boolean = false,
    val error: String? = null,
    val mensaje: String? = null,
)

@HiltViewModel
class ActividadEntregasViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val actividadRepository: ActividadRepository,
    private val fileDownloadManager: FileDownloadManager,
) : ViewModel() {

    private val actividadId: Int = checkNotNull(savedStateHandle["actividadId"])
    private val validador = ValidarFormulariosUseCase()

    private val _uiState = MutableStateFlow(ActividadEntregasUiState())
    val uiState: StateFlow<ActividadEntregasUiState> = _uiState.asStateFlow()

    init {
        cargarEntregas()
    }

    fun cargarEntregas() {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true) }
            actividadRepository.obtenerEntregasActividad(actividadId).fold(
                onSuccess = { grupo ->
                    _uiState.update {
                        it.copy(
                            actividad = grupo.actividad,
                            entregas = grupo.entregas,
                            cargando = false,
                        )
                    }
                },
                onFailure = { e -> _uiState.update { it.copy(cargando = false, error = e.message) } },
            )
        }
    }

    fun calificar(aprendizId: Int, notaTexto: String, retroalimentacion: String, exenta: Boolean) {
        val puntajeMax = _uiState.value.actividad?.puntajeMaximo ?: 5.0
        val err = validador.validarCalificacion(notaTexto, puntajeMax, exenta)
        if (err != null) {
            _uiState.update { it.copy(error = err) }
            return
        }
        val nota = if (exenta) null else notaTexto.replace(',', '.').toDouble()
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true) }
            actividadRepository.calificarEntrega(actividadId, aprendizId, nota, retroalimentacion, exenta).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            mensaje = if (exenta) "Actividad marcada como exenta." else "Calificación guardada."
                        )
                    }
                    cargarEntregas()
                },
                onFailure = { e -> _uiState.update { it.copy(cargando = false, error = e.message) } },
            )
        }
    }

    fun abrirArchivo(archivo: ArchivoAdjunto) {
        viewModelScope.launch {
            fileDownloadManager.descargarYAbrir(
                urlRelativaOCompleta = archivo.urlDescarga,
                nombreArchivo = archivo.nombreOriginal,
                mimeType = archivo.mimeType,
            ).onFailure { e ->
                _uiState.update { it.copy(error = e.message ?: "No se pudo abrir la evidencia.") }
            }
        }
    }

    fun limpiarFeedback() = _uiState.update { it.copy(error = null, mensaje = null) }
}
