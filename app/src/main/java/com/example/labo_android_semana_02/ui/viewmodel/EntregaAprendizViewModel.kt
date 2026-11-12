package com.example.labo_android_semana_02.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.labo_android_semana_02.data.remote.FileDownloadManager
import com.example.labo_android_semana_02.data.remote.MultipartHelper
import com.example.labo_android_semana_02.domain.model.Actividad
import com.example.labo_android_semana_02.domain.model.ArchivoAdjunto
import com.example.labo_android_semana_02.domain.model.ArchivoLocal
import com.example.labo_android_semana_02.domain.model.EstadoEntrega
import com.example.labo_android_semana_02.domain.repository.ActividadRepository
import com.example.labo_android_semana_02.domain.usecase.CalcularCalificacionCursoUseCase
import com.example.labo_android_semana_02.domain.usecase.ValidarFormulariosUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EntregaAprendizUiState(
    val actividad: Actividad? = null,
    val descripcion: String = "",
    val conservarArchivosPrevios: Boolean = true,
    val archivosNuevos: List<ArchivoLocal> = emptyList(),
    val cargando: Boolean = false,
    val enviando: Boolean = false,
    val error: String? = null,
    val mensaje: String? = null,
) {
    val bloqueadaPorCalificacion: Boolean
        get() = actividad?.miEntrega?.estado == EstadoEntrega.CALIFICADA ||
            actividad?.miEntrega?.estado == EstadoEntrega.EXENTA
}

@HiltViewModel
class EntregaAprendizViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val actividadRepository: ActividadRepository,
    private val multipartHelper: MultipartHelper,
    private val fileDownloadManager: FileDownloadManager,
) : ViewModel() {

    private val cursoId: Int = checkNotNull(savedStateHandle["cursoId"])
    private val actividadId: Int = checkNotNull(savedStateHandle["actividadId"])
    private val validador = ValidarFormulariosUseCase()
    private val calculador = CalcularCalificacionCursoUseCase()

    private val _uiState = MutableStateFlow(EntregaAprendizUiState())
    val uiState: StateFlow<EntregaAprendizUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            actividadRepository.observarActividades(cursoId).collect { list ->
                val act = list.firstOrNull { it.id == actividadId }
                if (act != null) {
                    _uiState.update { prev ->
                        val descInicial = if (prev.actividad == null) act.miEntrega?.descripcion.orEmpty() else prev.descripcion
                        prev.copy(actividad = act, descripcion = descInicial)
                    }
                }
            }
        }
        refrescar()
    }

    fun refrescar() {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true) }
            actividadRepository.sincronizarActividades(cursoId).fold(
                onSuccess = { _uiState.update { it.copy(cargando = false) } },
                onFailure = { e -> _uiState.update { it.copy(cargando = false, error = e.message) } },
            )
        }
    }

    fun onDescripcionChange(texto: String) = _uiState.update { it.copy(descripcion = texto, error = null) }
    fun onConservarArchivosChange(conservar: Boolean) = _uiState.update { it.copy(conservarArchivosPrevios = conservar) }

    fun agregarArchivo(uri: Uri) {
        val result = multipartHelper.leerYValidarUri(uri)
        result.fold(
            onSuccess = { archivo ->
                _uiState.update { prev ->
                    if (prev.archivosNuevos.size >= 10) {
                        prev.copy(error = "Máximo 10 archivos por envío.")
                    } else {
                        prev.copy(archivosNuevos = prev.archivosNuevos + archivo, error = null)
                    }
                }
            },
            onFailure = { e -> _uiState.update { it.copy(error = e.message) } },
        )
    }

    fun quitarArchivoNuevo(index: Int) {
        _uiState.update { prev ->
            prev.copy(archivosNuevos = prev.archivosNuevos.filterIndexed { i, _ -> i != index })
        }
    }

    fun enviarEntrega() {
        val st = _uiState.value
        if (st.bloqueadaPorCalificacion) {
            _uiState.update { it.copy(error = "Esta actividad ya fue calificada o exenta y no admite cambios.") }
            return
        }
        val teniaPrevios = !st.actividad?.miEntrega?.archivos.isNullOrEmpty()
        val errorVal = validador.validarEntrega(
            descripcion = st.descripcion,
            cantidadArchivosNuevos = st.archivosNuevos.size,
            conservarArchivosPrevios = st.conservarArchivosPrevios,
            teniaArchivosPrevios = teniaPrevios,
        )
        if (errorVal != null) {
            _uiState.update { it.copy(error = errorVal) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(enviando = true, error = null) }
            actividadRepository.enviarEntrega(
                actividadId = actividadId,
                descripcion = st.descripcion,
                conservarArchivos = st.conservarArchivosPrevios,
                archivos = st.archivosNuevos,
            ).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            enviando = false,
                            archivosNuevos = emptyList(),
                            mensaje = "Entrega enviada correctamente.",
                        )
                    }
                    refrescar()
                },
                onFailure = { e -> _uiState.update { it.copy(enviando = false, error = e.message) } },
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
                _uiState.update { it.copy(error = e.message ?: "No se pudo abrir el archivo.") }
            }
        }
    }

    fun convertirNotaA05Y0100(nota: Double, puntajeMax: Double): Pair<Double, Double> {
        val max = if (puntajeMax > 0.0) puntajeMax else 5.0
        val nota05 = ((nota / max) * 5.0).coerceIn(0.0, 5.0)
        return nota05 to calculador.convertir05A100(nota05)
    }

    fun limpiarFeedback() = _uiState.update { it.copy(error = null, mensaje = null) }
}
