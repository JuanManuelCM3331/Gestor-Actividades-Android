package com.example.labo_android_semana_02.domain.usecase

import com.example.labo_android_semana_02.domain.Prioridad
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

data class ResultadoValidacionFormulario(
    val errores: Map<String, String>,
    val puedeGuardar: Boolean
)

class ValidarFormularioReporteUseCase @Inject constructor() {

    operator fun invoke(
        titulo: String,
        descripcion: String,
        fecha: String,
        @Suppress("UNUSED_PARAMETER") prioridad: Prioridad,
        progreso: String,
        referenciaHoyMillis: Long? = null
    ): ResultadoValidacionFormulario {
        val errores = mutableMapOf<String, String>()

        if (titulo.isBlank() || titulo.length < 3) {
            errores["titulo"] = "Título inválido (min 3)"
        }
        if (titulo.length > 80) {
            errores["titulo"] = "Máximo 80 caracteres"
        }
        if (descripcion.length > 240) {
            errores["descripcion"] = "Máximo 240 caracteres"
        }

        val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
            isLenient = false
        }
        try {
            if (fecha.isNotBlank()) {
                val date = format.parse(fecha)
                val cal = Calendar.getInstance().apply {
                    if (referenciaHoyMillis != null) {
                        timeInMillis = referenciaHoyMillis
                    }
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                if (date != null && date.before(cal.time)) {
                    errores["fecha"] = "No puede ser anterior a hoy"
                }
            } else {
                errores["fecha"] = "Fecha obligatoria"
            }
        } catch (_: Exception) {
            errores["fecha"] = "Formato dd/mm/aaaa"
        }

        val progInt = progreso.toIntOrNull()
        if (progInt == null || progInt !in 0..100) {
            errores["progreso"] = "Rango 0-100"
        }

        return ResultadoValidacionFormulario(
            errores = errores,
            puedeGuardar = errores.isEmpty()
        )
    }
}
