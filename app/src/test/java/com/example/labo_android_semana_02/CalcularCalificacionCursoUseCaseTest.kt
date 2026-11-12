package com.example.labo_android_semana_02

import com.example.labo_android_semana_02.domain.model.Actividad
import com.example.labo_android_semana_02.domain.model.EntregaDetalle
import com.example.labo_android_semana_02.domain.model.EstadoEntrega
import com.example.labo_android_semana_02.domain.usecase.CalcularCalificacionCursoUseCase
import org.junit.Assert.assertEquals
import org.junit.Test

class CalcularCalificacionCursoUseCaseTest {

    private val useCase = CalcularCalificacionCursoUseCase()

    private fun crearActividad(
        id: Int,
        puntajeMax: Double = 5.0,
        fechaLimite: Long = 2_000_000L,
        entrega: EntregaDetalle? = null,
    ) = Actividad(
        id = id,
        cursoId = 1,
        titulo = "Actividad $id",
        descripcion = "Desc",
        fechaLimite = fechaLimite,
        puntajeMaximo = puntajeMax,
        adjuntos = emptyList(),
        totalEntregas = 0,
        porCalificar = 0,
        estadoAprendiz = entrega?.estado,
        miEntrega = entrega,
        createdAt = 1L,
        updatedAt = 1L,
    )

    @Test
    fun `actividades sin calificar cuentan como cero y exentas se excluyen`() {
        val actividades = listOf(
            crearActividad(
                id = 1,
                entrega = EntregaDetalle(
                    id = 10, actividadId = 1, aprendizId = 7, descripcion = "Ok",
                    nota = 4.0, retroalimentacion = "Bien", exenta = false,
                    estado = EstadoEntrega.CALIFICADA, fechaEntrega = 100L, fechaCalificacion = 200L, archivos = emptyList(),
                ),
            ),
            crearActividad(id = 2, fechaLimite = 500L, entrega = null),
            crearActividad(
                id = 3,
                entrega = EntregaDetalle(
                    id = 11, actividadId = 3, aprendizId = 7, descripcion = "Presencial",
                    nota = null, retroalimentacion = "Entregada por fuera", exenta = true,
                    estado = EstadoEntrega.EXENTA, fechaEntrega = null, fechaCalificacion = 200L, archivos = emptyList(),
                ),
            ),
        )

        val resumen = useCase(actividades, ahoraMs = 1_000L)

        assertEquals(3, resumen.totalActividades)
        assertEquals(2, resumen.actividadesEvaluables)
        assertEquals(1, resumen.actividadesCalificadas)
        assertEquals(1, resumen.actividadesExentas)
        // (4.0 + 0.0) / 2 = 2.00 -> 40.00 / 100
        assertEquals(2.0, resumen.nota05, 0.01)
        assertEquals(40.0, resumen.nota0100, 0.01)
        assertEquals(EstadoEntrega.TARDIA, resumen.detalle[1].estado)
        assertEquals(EstadoEntrega.EXENTA, resumen.detalle[2].estado)
    }

    @Test
    fun `normaliza puntajes maximos distintos a escala 0-5 y convierte a 0-100`() {
        val act = crearActividad(
            id = 5,
            puntajeMax = 100.0,
            entrega = EntregaDetalle(
                id = 20, actividadId = 5, aprendizId = 7, descripcion = "Quiz",
                nota = 90.0, retroalimentacion = null, exenta = false,
                estado = EstadoEntrega.CALIFICADA, fechaEntrega = 100L, fechaCalificacion = 200L, archivos = emptyList(),
            ),
        )

        val resumen = useCase(listOf(act), ahoraMs = 1_000L)

        assertEquals(4.5, resumen.nota05, 0.01)
        assertEquals(90.0, resumen.nota0100, 0.01)
        assertEquals(60.0, useCase.convertir05A100(3.0), 0.01)
    }
}
