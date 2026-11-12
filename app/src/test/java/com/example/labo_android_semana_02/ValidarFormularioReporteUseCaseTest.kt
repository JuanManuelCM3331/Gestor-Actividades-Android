package com.example.labo_android_semana_02

import com.example.labo_android_semana_02.domain.Prioridad
import com.example.labo_android_semana_02.domain.usecase.ValidarFormularioReporteUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ValidarFormularioReporteUseCaseTest {

    private val useCase = ValidarFormularioReporteUseCase()

    @Test
    fun `formulario valido pasa sin errores`() {
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 3) }
        val fecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(cal.time)

        val resultado = useCase(
            titulo = "Reparar proyector",
            descripcion = "Aula 204",
            fecha = fecha,
            prioridad = Prioridad.ALTA,
            progreso = "50"
        )

        assertTrue(resultado.puedeGuardar)
        assertTrue(resultado.errores.isEmpty())
    }

    @Test
    fun `titulo corto y fecha pasada producen errores esperados`() {
        val cal = Calendar.getInstance()
        val hoyMillis = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, -2)
        val fechaPasada = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(cal.time)

        val resultado = useCase(
            titulo = "ab",
            descripcion = "",
            fecha = fechaPasada,
            prioridad = Prioridad.MEDIA,
            progreso = "150",
            referenciaHoyMillis = hoyMillis
        )

        assertFalse(resultado.puedeGuardar)
        assertEquals("Título inválido (min 3)", resultado.errores["titulo"])
        assertEquals("No puede ser anterior a hoy", resultado.errores["fecha"])
        assertEquals("Rango 0-100", resultado.errores["progreso"])
    }
}
