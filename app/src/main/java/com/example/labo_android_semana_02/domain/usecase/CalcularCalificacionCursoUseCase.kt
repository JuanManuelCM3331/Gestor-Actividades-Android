package com.example.labo_android_semana_02.domain.usecase

import com.example.labo_android_semana_02.domain.model.Actividad
import com.example.labo_android_semana_02.domain.model.DetalleCalificacionActividad
import com.example.labo_android_semana_02.domain.model.EstadoEntrega
import com.example.labo_android_semana_02.domain.model.ResumenCalificacionEstudiante
import kotlin.math.roundToInt

/**
 * Calcula el promedio ponderado simple del curso en escala Colombia (0.00 a 5.00)
 * con conversión automática a escala 0.00 a 100.00.
 * Las actividades exentas se excluyen; las actividades no exentas sin calificación cuentan como 0.
 */
class CalcularCalificacionCursoUseCase {

    operator fun invoke(
        actividades: List<Actividad>,
        ahoraMs: Long = System.currentTimeMillis(),
    ): ResumenCalificacionEstudiante {
        var sumaNormalizada05 = 0.0
        var evaluables = 0
        var calificadas = 0
        var exentas = 0
        val detalle = mutableListOf<DetalleCalificacionActividad>()

        for (act in actividades) {
            val entrega = act.miEntrega
            val puntajeMax = if (act.puntajeMaximo > 0.0) act.puntajeMaximo else 5.0
            val esExenta = entrega?.exenta == true || entrega?.estado == EstadoEntrega.EXENTA

            if (esExenta) {
                exentas += 1
                detalle.add(
                    DetalleCalificacionActividad(
                        actividadId = act.id,
                        titulo = act.titulo,
                        puntajeMaximo = puntajeMax,
                        nota = null,
                        notaEscala05 = null,
                        estado = EstadoEntrega.EXENTA,
                        exenta = true,
                        retroalimentacion = entrega?.retroalimentacion,
                    )
                )
                continue
            }

            evaluables += 1
            var nota05 = 0.0
            var estado = entrega?.estado ?: if (ahoraMs > act.fechaLimite) EstadoEntrega.TARDIA else EstadoEntrega.PENDIENTE
            val notaOriginal = entrega?.nota

            if (estado == EstadoEntrega.CALIFICADA && notaOriginal != null) {
                calificadas += 1
                nota05 = ((notaOriginal / puntajeMax) * 5.0).coerceIn(0.0, 5.0)
            }

            sumaNormalizada05 += nota05
            detalle.add(
                DetalleCalificacionActividad(
                    actividadId = act.id,
                    titulo = act.titulo,
                    puntajeMaximo = puntajeMax,
                    nota = notaOriginal,
                    notaEscala05 = redondear2(nota05),
                    estado = estado,
                    exenta = false,
                    retroalimentacion = entrega?.retroalimentacion,
                )
            )
        }

        val promedio05 = if (evaluables > 0) redondear2(sumaNormalizada05 / evaluables) else 0.0
        val promedio0100 = convertir05A100(promedio05)

        return ResumenCalificacionEstudiante(
            aprendiz = null,
            nota05 = promedio05,
            nota0100 = promedio0100,
            totalActividades = actividades.size,
            actividadesEvaluables = evaluables,
            actividadesCalificadas = calificadas,
            actividadesExentas = exentas,
            detalle = detalle,
        )
    }

    fun convertir05A100(nota05: Double): Double {
        val bounded = nota05.coerceIn(0.0, 5.0)
        return redondear2((bounded / 5.0) * 100.0)
    }

    private fun redondear2(value: Double): Double = (value * 100.0).roundToInt() / 100.0
}
