package com.example.labo_android_semana_02.domain.usecase

import com.example.labo_android_semana_02.domain.model.ArchivoLocal

class ValidarFormulariosUseCase {

    private val emailRegex = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    private val urlRegex = Regex("^https?://.+", RegexOption.IGNORE_CASE)

    fun validarLogin(email: String, password: String): String? {
        if (!emailRegex.matches(email.trim())) return "Ingresa un correo electrónico válido."
        if (password.isEmpty()) return "Ingresa tu contraseña."
        return null
    }

    fun validarRegistro(nombre: String, email: String, password: String): String? {
        if (nombre.trim().length < 3) return "El nombre completo debe tener al menos 3 caracteres."
        if (!emailRegex.matches(email.trim())) return "Ingresa un correo electrónico válido."
        if (password.length < 6) return "La contraseña debe tener mínimo 6 caracteres."
        return null
    }

    fun validarCurso(nombre: String, descripcion: String): String? {
        val clean = nombre.trim()
        if (clean.length < 3 || clean.length > 120) {
            return "El nombre del curso debe tener entre 3 y 120 caracteres."
        }
        if (descripcion.trim().isEmpty()) {
            return "Agrega una breve descripción para el curso."
        }
        return null
    }

    fun validarCodigoCurso(codigo: String): String? {
        val clean = codigo.trim()
        if (clean.length < 4) return "Ingresa un código de acceso válido (mínimo 4 caracteres)."
        return null
    }

    fun validarActividad(titulo: String, puntajeMaximoTexto: String): String? {
        val clean = titulo.trim()
        if (clean.length < 3 || clean.length > 140) {
            return "El título de la actividad debe tener entre 3 y 140 caracteres."
        }
        val puntaje = puntajeMaximoTexto.replace(',', '.').toDoubleOrNull()
        if (puntaje == null || puntaje <= 0.0 || puntaje > 1000.0) {
            return "Ingresa un puntaje máximo mayor a 0 y hasta 1000."
        }
        return null
    }

    fun validarCalificacion(notaTexto: String, puntajeMaximo: Double, exenta: Boolean): String? {
        if (exenta) return null
        val nota = notaTexto.replace(',', '.').toDoubleOrNull()
            ?: return "Ingresa una nota numérica válida."
        if (nota < 0.0 || nota > puntajeMaximo) {
            return "La nota debe estar entre 0 y $puntajeMaximo."
        }
        return null
    }

    fun validarMaterial(
        titulo: String,
        esEnlace: Boolean,
        enlaceUrl: String,
        archivo: ArchivoLocal?,
    ): String? {
        val clean = titulo.trim()
        if (clean.length < 3 || clean.length > 140) {
            return "El título del material debe tener entre 3 y 140 caracteres."
        }
        if (esEnlace) {
            if (!urlRegex.matches(enlaceUrl.trim())) {
                return "Ingresa un enlace HTTP o HTTPS válido."
            }
        } else if (archivo == null) {
            return "Selecciona un archivo para subir como material."
        }
        return null
    }

    fun validarEntrega(
        descripcion: String,
        cantidadArchivosNuevos: Int,
        conservarArchivosPrevios: Boolean,
        teniaArchivosPrevios: Boolean,
    ): String? {
        val hayArchivos = cantidadArchivosNuevos > 0 || (conservarArchivosPrevios && teniaArchivosPrevios)
        if (descripcion.trim().isEmpty() && !hayArchivos) {
            return "Agrega una descripción o adjunta al menos un archivo de evidencia."
        }
        return null
    }

    fun validarHiloForo(
        titulo: String,
        contenido: String,
        esPrivado: Boolean,
        esInstructor: Boolean,
        aprendizObjetivoId: Int?,
    ): String? {
        val cleanTitulo = titulo.trim()
        if (cleanTitulo.length < 3 || cleanTitulo.length > 160) {
            return "El título del hilo debe tener entre 3 y 160 caracteres."
        }
        if (contenido.trim().length < 2) {
            return "Escribe el contenido del mensaje (mínimo 2 caracteres)."
        }
        if (esPrivado && esInstructor && aprendizObjetivoId == null) {
            return "Selecciona el aprendiz para iniciar la conversación privada."
        }
        return null
    }

    fun validarMensajeChatForo(
        contenido: String,
        enlaceUrl: String,
        cantidadArchivos: Int,
        esPrivado: Boolean,
        esInstructor: Boolean,
        aprendizObjetivoId: Int?,
    ): String? {
        if (esPrivado && esInstructor && aprendizObjetivoId == null) {
            return "Selecciona un aprendiz para enviarle un mensaje privado."
        }
        val cleanUrl = enlaceUrl.trim()
        if (cleanUrl.isNotEmpty() && !urlRegex.matches(cleanUrl)) {
            return "Ingresa un enlace válido que comience con http:// o https://"
        }
        if (contenido.trim().isEmpty() && cleanUrl.isEmpty() && cantidadArchivos <= 0) {
            return "Escribe un mensaje, comparte un enlace o adjunta un archivo, audio o imagen."
        }
        return null
    }
}
