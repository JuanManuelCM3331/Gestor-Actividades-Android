package com.example.labo_android_semana_02.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.labo_android_semana_02.domain.model.HiloForo
import com.example.labo_android_semana_02.domain.model.MensajeForo
import com.example.labo_android_semana_02.domain.repository.ForoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ForoHiloUiState(
    val hilo: HiloForo? = null,
    val mensajes: List<MensajeForo> = emptyList(),
    val respuestaTexto: String = "",
    val cargando: Boolean = false,
    val enviando: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class ForoHiloViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val foroRepository: ForoRepository,
) : ViewModel() {

    private val hiloId: Int = checkNotNull(savedStateHandle["hiloId"])
    private val _uiState = MutableStateFlow(ForoHiloUiState())
    val uiState: StateFlow<ForoHiloUiState> = _uiState.asStateFlow()

    init {
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true) }
            foroRepository.obtenerDetalleHilo(hiloId).fold(
                onSuccess = { detalle ->
                    _uiState.update {
                        it.copy(
                            hilo = detalle.hilo,
                            mensajes = detalle.mensajes,
                            cargando = false,
                        )
                    }
                },
                onFailure = { e -> _uiState.update { it.copy(cargando = false, error = e.message) } },
            )
        }
    }

    fun onRespuestaChange(texto: String) = _uiState.update { it.copy(respuestaTexto = texto, error = null) }

    fun enviarRespuesta() {
        val clean = _uiState.value.respuestaTexto.trim()
        if (clean.isEmpty()) {
            _uiState.update { it.copy(error = "Escribe una respuesta antes de enviar.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(enviando = true, error = null) }
            foroRepository.responderHilo(hiloId, clean).fold(
                onSuccess = { nuevo ->
                    _uiState.update { prev ->
                        prev.copy(
                            enviando = false,
                            respuestaTexto = "",
                            mensajes = prev.mensajes + nuevo,
                        )
                    }
                },
                onFailure = { e -> _uiState.update { it.copy(enviando = false, error = e.message) } },
            )
        }
    }

    fun limpiarError() = _uiState.update { it.copy(error = null) }
}
