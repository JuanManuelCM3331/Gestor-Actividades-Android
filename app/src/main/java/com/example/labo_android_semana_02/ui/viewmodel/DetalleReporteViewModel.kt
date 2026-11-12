package com.example.labo_android_semana_02.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.labo_android_semana_02.domain.Reporte
import com.example.labo_android_semana_02.domain.repository.ReporteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class DetalleReporteViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    reporteRepository: ReporteRepository
) : ViewModel() {

    private val reporteId: Int = savedStateHandle.get<Int>("reporteId") ?: -1

    val reporte: StateFlow<Reporte?> = if (reporteId != -1) {
        reporteRepository.getReporteById(reporteId)
    } else {
        flowOf(null)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null
    )
}
