package com.example.labo_android_semana_02.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.labo_android_semana_02.domain.repository.PreferenciasRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TemaViewModel @Inject constructor(
    private val preferenciasRepository: PreferenciasRepository,
) : ViewModel() {

    val temaOscuro: StateFlow<Boolean> = preferenciasRepository.temaOscuroFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false,
    )

    fun alternarTema() {
        viewModelScope.launch {
            preferenciasRepository.guardarTemaOscuro(!temaOscuro.value)
        }
    }
}
