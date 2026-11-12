package com.example.labo_android_semana_02.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.labo_android_semana_02.domain.model.AuthSession
import com.example.labo_android_semana_02.domain.model.RolUsuario
import com.example.labo_android_semana_02.domain.repository.AuthRepository
import com.example.labo_android_semana_02.domain.usecase.ValidarFormulariosUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AuthSessionState {
    data object Loading : AuthSessionState
    data object Unauthenticated : AuthSessionState
    data class Authenticated(val session: AuthSession) : AuthSessionState
}

data class AuthUiState(
    val esRegistro: Boolean = false,
    val nombre: String = "",
    val email: String = "",
    val password: String = "",
    val rolSeleccionado: RolUsuario = RolUsuario.APRENDIZ,
    val cargando: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val validador = ValidarFormulariosUseCase()

    val sessionState: StateFlow<AuthSessionState> = authRepository.sessionFlow
        .map { session ->
            if (session == null) AuthSessionState.Unauthenticated
            else AuthSessionState.Authenticated(session)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AuthSessionState.Loading,
        )

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onNombreChange(value: String) = _uiState.update { it.copy(nombre = value, error = null) }
    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value, error = null) }
    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, error = null) }
    fun onRolChange(rol: RolUsuario) = _uiState.update { it.copy(rolSeleccionado = rol, error = null) }
    fun alternarModo() = _uiState.update { it.copy(esRegistro = !it.esRegistro, error = null) }
    fun limpiarError() = _uiState.update { it.copy(error = null) }

    fun enviarFormulario() {
        val state = _uiState.value
        val errorLocal = if (state.esRegistro) {
            validador.validarRegistro(state.nombre, state.email, state.password)
        } else {
            validador.validarLogin(state.email, state.password)
        }
        if (errorLocal != null) {
            _uiState.update { it.copy(error = errorLocal) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, error = null) }
            val result = if (state.esRegistro) {
                authRepository.register(
                    nombre = state.nombre,
                    email = state.email,
                    password = state.password,
                    rol = state.rolSeleccionado,
                )
            } else {
                authRepository.login(email = state.email, password = state.password)
            }
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(cargando = false, password = "", error = null) }
                },
                onFailure = { err ->
                    _uiState.update { it.copy(cargando = false, error = err.message ?: "Error al autenticar.") }
                },
            )
        }
    }

    fun cerrarSesion() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }
}
