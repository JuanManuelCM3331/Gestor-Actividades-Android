package com.example.labo_android_semana_02.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.labo_android_semana_02.domain.ActividadFormativa
import com.example.labo_android_semana_02.domain.repository.ActividadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class DetalleActividadViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    actividadRepository: ActividadRepository
) : ViewModel() {

    private val actividadId: Int = savedStateHandle.get<Int>("actividadId") ?: -1

    val actividad: StateFlow<ActividadFormativa?> = if (actividadId != -1) {
        actividadRepository.getActividadById(actividadId)
    } else {
        flowOf(null)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null
    )
}
