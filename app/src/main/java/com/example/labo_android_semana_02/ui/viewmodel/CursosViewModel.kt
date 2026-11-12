package com.example.labo_android_semana_02.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.labo_android_semana_02.domain.model.Curso
import com.example.labo_android_semana_02.domain.model.EstadoCurso
import com.example.labo_android_semana_02.domain.model.Usuario
import com.example.labo_android_semana_02.domain.repository.AuthRepository
import com.example.labo_android_semana_02.domain.repository.CursoRepository
import com.example.labo_android_semana_02.domain.usecase.ValidarFormulariosUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class FiltroEstadoCurso { TODOS, ACTIVO, ARCHIVADO }

data class CursosUiState(
    val usuario: Usuario? = null,
    val todosLosCursos: List<Curso> = emptyList(),
    val filtro: FiltroEstadoCurso = FiltroEstadoCurso.TODOS,
    val busqueda: String = "",
    val cargando: Boolean = false,
    val error: String? = null,
    val mensaje: String? = null,
) {
    val cursosFiltrados: List<Curso>
        get() = todosLosCursos.filter { c ->
            val cumpleFiltro = when (filtro) {
                FiltroEstadoCurso.TODOS -> true
                FiltroEstadoCurso.ACTIVO -> c.estado == EstadoCurso.ACTIVO
                FiltroEstadoCurso.ARCHIVADO -> c.estado == EstadoCurso.ARCHIVADO
            }
            val q = busqueda.trim()
            val cumpleBusqueda = q.isEmpty() ||
                c.nombre.contains(q, ignoreCase = true) ||
                c.descripcion.contains(q, ignoreCase = true) ||
                c.instructorNombre.contains(q, ignoreCase = true)
            cumpleFiltro && cumpleBusqueda
        }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CursosViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val cursoRepository: CursoRepository,
) : ViewModel() {

    private val validador = ValidarFormulariosUseCase()
    private val _uiState = MutableStateFlow(CursosUiState())
    val uiState: StateFlow<CursosUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.sessionFlow
                .flatMapLatest { session ->
                    val usuario = session?.usuario
                    _uiState.update { it.copy(usuario = usuario) }
                    if (usuario == null) {
                        flowOf(emptyList())
                    } else {
                        launch { refrescarInterno(usuario) }
                        cursoRepository.observarCursos(usuario.id, usuario.rol)
                    }
                }
                .collect { lista ->
                    _uiState.update { it.copy(todosLosCursos = lista) }
                }
        }
    }

    fun refrescar() {
        val usuario = _uiState.value.usuario ?: return
        viewModelScope.launch { refrescarInterno(usuario) }
    }

    private suspend fun refrescarInterno(usuario: Usuario) {
        _uiState.update { it.copy(cargando = true) }
        cursoRepository.sincronizarCursos(usuario.id, usuario.rol).fold(
            onSuccess = { _uiState.update { it.copy(cargando = false) } },
            onFailure = { err -> _uiState.update { it.copy(cargando = false, error = err.message) } },
        )
    }

    fun cambiarFiltro(filtro: FiltroEstadoCurso) = _uiState.update { it.copy(filtro = filtro) }
    fun cambiarBusqueda(texto: String) = _uiState.update { it.copy(busqueda = texto) }
    fun limpiarFeedback() = _uiState.update { it.copy(error = null, mensaje = null) }

    fun crearCurso(nombre: String, descripcion: String, estado: EstadoCurso) {
        val err = validador.validarCurso(nombre, descripcion)
        if (err != null) {
            _uiState.update { it.copy(error = err) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, error = null) }
            cursoRepository.crearCurso(nombre, descripcion, estado).fold(
                onSuccess = { creado ->
                    _uiState.update { it.copy(mensaje = "Curso creado. Código: ${creado.codigoAcceso}") }
                    refrescar()
                },
                onFailure = { e -> _uiState.update { it.copy(cargando = false, error = e.message) } },
            )
        }
    }

    fun editarCurso(cursoId: Int, nombre: String, descripcion: String, estado: EstadoCurso) {
        val err = validador.validarCurso(nombre, descripcion)
        if (err != null) {
            _uiState.update { it.copy(error = err) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, error = null) }
            cursoRepository.actualizarCurso(cursoId, nombre, descripcion, estado).fold(
                onSuccess = {
                    _uiState.update { it.copy(mensaje = "Curso actualizado correctamente.") }
                    refrescar()
                },
                onFailure = { e -> _uiState.update { it.copy(cargando = false, error = e.message) } },
            )
        }
    }

    fun eliminarCurso(cursoId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, error = null) }
            cursoRepository.eliminarCurso(cursoId).fold(
                onSuccess = {
                    _uiState.update { it.copy(mensaje = "Curso eliminado.") }
                    refrescar()
                },
                onFailure = { e -> _uiState.update { it.copy(cargando = false, error = e.message) } },
            )
        }
    }

    fun unirseACurso(codigoAcceso: String) {
        val err = validador.validarCodigoCurso(codigoAcceso)
        if (err != null) {
            _uiState.update { it.copy(error = err) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, error = null) }
            cursoRepository.unirseACurso(codigoAcceso).fold(
                onSuccess = {
                    _uiState.update { it.copy(mensaje = "Te uniste al curso correctamente.") }
                    refrescar()
                },
                onFailure = { e -> _uiState.update { it.copy(cargando = false, error = e.message) } },
            )
        }
    }

    fun salirseDeCurso(cursoId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, error = null) }
            cursoRepository.salirseDeCurso(cursoId).fold(
                onSuccess = {
                    _uiState.update { it.copy(mensaje = "Saliste del curso.") }
                    refrescar()
                },
                onFailure = { e -> _uiState.update { it.copy(cargando = false, error = e.message) } },
            )
        }
    }

    fun cerrarSesion() {
        viewModelScope.launch { authRepository.logout() }
    }
}
