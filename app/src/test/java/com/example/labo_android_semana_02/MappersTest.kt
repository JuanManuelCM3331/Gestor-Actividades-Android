package com.example.labo_android_semana_02

import com.example.labo_android_semana_02.data.mapper.toDomain
import com.example.labo_android_semana_02.data.mapper.toEntity
import com.example.labo_android_semana_02.data.mapper.toRequestDto
import com.example.labo_android_semana_02.data.remote.dto.ActividadDto
import com.example.labo_android_semana_02.data.remote.dto.CategoriaDto
import com.example.labo_android_semana_02.data.remote.dto.ReporteDto
import com.example.labo_android_semana_02.domain.Prioridad
import com.example.labo_android_semana_02.domain.Reporte
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MappersTest {

    @Test
    fun `ReporteDto se mapea a Entity y Domain preservando categoriaNombre y sincronizado`() {
        val dto = ReporteDto(
            id = 42,
            titulo = "Falla eléctrica",
            descripcion = "Bloque B",
            fecha = 1700000000000L,
            categoriaId = 2,
            categoriaNombre = "Infraestructura",
            categoriaIcono = "🏗️",
            resuelto = true,
            evidenciaFotoUri = "file:///foto.jpg",
            evidenciaArchivoUri = null
        )

        val domain = dto.toEntity().toDomain()

        assertEquals(42, domain.id)
        assertEquals(2, domain.categoriaId)
        assertEquals("Infraestructura", domain.categoriaNombre)
        assertTrue(domain.resuelto)
        assertTrue(domain.sincronizado)
        assertEquals("file:///foto.jpg", domain.evidenciaFotoUri)
    }

    @Test
    fun `ActividadDto mapea prioridad valida y fallback a MEDIA`() {
        val alta = ActividadDto(1, "A", "D", 1000L, 50, false, prioridad = "ALTA").toEntity()
        val invalida = ActividadDto(2, "B", "D", 1000L, 0, false, prioridad = "DESCONOCIDA").toEntity()

        assertEquals(Prioridad.ALTA, alta.prioridad)
        assertEquals(Prioridad.MEDIA, invalida.prioridad)
    }

    @Test
    fun `CategoriaDto y Reporte request se mapean correctamente`() {
        val cat = CategoriaDto(3, "Académico", "📚").toEntity().toDomain()
        assertEquals("Académico", cat.nombre)

        val req = Reporte(0, "Test", "Desc", 999L, categoriaId = 3, resuelto = false).toRequestDto()
        assertEquals(3, req.categoriaId)
        assertEquals("Test", req.titulo)
    }
}
