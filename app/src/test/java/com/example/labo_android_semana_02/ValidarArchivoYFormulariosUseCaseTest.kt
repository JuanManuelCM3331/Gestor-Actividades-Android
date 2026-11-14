package com.example.labo_android_semana_02

import com.example.labo_android_semana_02.domain.model.ArchivoLocal
import com.example.labo_android_semana_02.domain.usecase.ValidarArchivoUseCase
import com.example.labo_android_semana_02.domain.usecase.ValidarFormulariosUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidarArchivoYFormulariosUseCaseTest {

    private val validarArchivo = ValidarArchivoUseCase()
    private val validarFormularios = ValidarFormulariosUseCase()

    @Test
    fun `valida limite de 20 MB y extensiones permitidas`() {
        val exacto = ArchivoLocal("content://1", "taller.pdf", "application/pdf", ValidarArchivoUseCase.MAX_FILE_SIZE_BYTES)
        assertTrue(validarArchivo(exacto).esValido)

        val excedido = ArchivoLocal("content://2", "video.mp4", "video/mp4", ValidarArchivoUseCase.MAX_FILE_SIZE_BYTES + 1L)
        val resExcedido = validarArchivo(excedido)
        assertFalse(resExcedido.esValido)
        assertNotNull(resExcedido.mensajeError)

        val peligroso = ArchivoLocal("content://3", "instalador.exe", "application/octet-stream", 1024L)
        assertFalse(validarArchivo(peligroso).esValido)

        val vacio = ArchivoLocal("content://4", "notas.txt", "text/plain", 0L)
        assertFalse(validarArchivo(vacio).esValido)

        val audioGrabado = ArchivoLocal("file:///cache/nota_voz_123.m4a", "nota_voz_123.m4a", "audio/mp4", 48_000L)
        assertTrue(validarArchivo(audioGrabado).esValido)

        val audioOgg = ArchivoLocal("content://5", "mensaje.ogg", "audio/ogg", 64_000L)
        assertTrue(validarArchivo(audioOgg).esValido)
    }

    @Test
    fun `valida formularios de auth curso actividad calificacion material entrega y foro`() {
        assertNotNull(validarFormularios.validarLogin("invalido", "123"))
        assertNull(validarFormularios.validarLogin("docente@ctma.edu.co", "Secret123"))

        assertNotNull(validarFormularios.validarRegistro("Ab", "a@b.co", "123456"))
        assertNotNull(validarFormularios.validarRegistro("Ana López", "a@b.co", "123"))
        assertNull(validarFormularios.validarRegistro("Ana López", "a@b.co", "123456"))

        assertNotNull(validarFormularios.validarCurso("AB", "Descripción"))
        assertNull(validarFormularios.validarCurso("Programación Web", "Curso práctico"))

        assertNotNull(validarFormularios.validarCalificacion("5.5", 5.0, exenta = false))
        assertNull(validarFormularios.validarCalificacion("", 5.0, exenta = true))
        assertNull(validarFormularios.validarCalificacion("4.8", 5.0, exenta = false))

        assertNotNull(validarFormularios.validarMaterial("Guía", esEnlace = true, enlaceUrl = "ftp://bad", archivo = null))
        assertNull(validarFormularios.validarMaterial("Guía", esEnlace = true, enlaceUrl = "https://developer.android.com", archivo = null))

        assertNotNull(validarFormularios.validarEntrega("  ", 0, conservarArchivosPrevios = false, teniaArchivosPrevios = false))
        assertNull(validarFormularios.validarEntrega("Adjunto solución", 0, conservarArchivosPrevios = false, teniaArchivosPrevios = false))

        val errorPrivadoSinTarget = validarFormularios.validarHiloForo(
            titulo = "Tutoría",
            contenido = "Revisemos tu taller",
            esPrivado = true,
            esInstructor = true,
            aprendizObjetivoId = null,
        )
        assertEquals("Selecciona el aprendiz para iniciar la conversación privada.", errorPrivadoSinTarget)

        // Validación del nuevo chat convencional del foro
        assertNotNull(
            validarFormularios.validarMensajeChatForo(
                contenido = "   ",
                enlaceUrl = "",
                cantidadArchivos = 0,
                esPrivado = false,
                esInstructor = false,
                aprendizObjetivoId = null,
            ),
        )
        assertNull(
            validarFormularios.validarMensajeChatForo(
                contenido = "",
                enlaceUrl = "https://kotlinlang.org",
                cantidadArchivos = 0,
                esPrivado = false,
                esInstructor = false,
                aprendizObjetivoId = null,
            ),
        )
        assertNull(
            validarFormularios.validarMensajeChatForo(
                contenido = "",
                enlaceUrl = "",
                cantidadArchivos = 1,
                esPrivado = true,
                esInstructor = true,
                aprendizObjetivoId = 2,
            ),
        )
        assertNotNull(
            validarFormularios.validarMensajeChatForo(
                contenido = "Hola",
                enlaceUrl = "ftp://invalido",
                cantidadArchivos = 0,
                esPrivado = false,
                esInstructor = false,
                aprendizObjetivoId = null,
            ),
        )
    }
}
