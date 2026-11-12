package com.example.labo_android_semana_02.domain.usecase

import com.example.labo_android_semana_02.domain.Reporte
import javax.inject.Inject

data class ResumenReportes(
    val pendientes: Int = 0,
    val completados: Int = 0
) {
    val total: Int get() = pendientes + completados
}

class CalcularResumenReportesUseCase @Inject constructor() {
    operator fun invoke(reportes: List<Reporte>): ResumenReportes = ResumenReportes(
        pendientes = reportes.count { !it.resuelto },
        completados = reportes.count { it.resuelto }
    )
}
