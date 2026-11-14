package com.example.labo_android_semana_02.domain.usecase

import com.example.labo_android_semana_02.domain.model.ArchivoLocal

data class ResultadoValidacionArchivo(
    val esValido: Boolean,
    val mensajeError: String? = null,
)

/**
 * Valida límite de tamaño (20 MB) y extensiones permitidas sincronizadas con el backend.
 */
class ValidarArchivoUseCase {

    companion object {
        const val MAX_FILE_SIZE_MB = 20L
        const val MAX_FILE_SIZE_BYTES = MAX_FILE_SIZE_MB * 1024L * 1024L

        val EXTENSIONES_PERMITIDAS = setOf(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "csv",
            "jpg", "jpeg", "png", "webp", "gif",
            "mp3", "wav", "m4a", "aac", "ogg", "opus", "3gp", "amr", "mp4", "webm",
            "zip", "rar",
        )
    }

    operator fun invoke(archivo: ArchivoLocal): ResultadoValidacionArchivo {
        if (archivo.nombre.isBlank()) {
            return ResultadoValidacionArchivo(false, "El archivo no tiene un nombre válido.")
        }

        val extension = archivo.nombre.substringAfterLast('.', "").lowercase()
        if (extension.isEmpty() || extension !in EXTENSIONES_PERMITIDAS) {
            return ResultadoValidacionArchivo(
                false,
                "Formato no permitido (.$extension) en '${archivo.nombre}'.",
            )
        }

        if (archivo.tamanoBytes <= 0L) {
            return ResultadoValidacionArchivo(
                false,
                "El archivo '${archivo.nombre}' está vacío o no pudo leerse.",
            )
        }

        if (archivo.tamanoBytes > MAX_FILE_SIZE_BYTES) {
            return ResultadoValidacionArchivo(
                false,
                "'${archivo.nombre}' supera el límite máximo de $MAX_FILE_SIZE_MB MB.",
            )
        }

        return ResultadoValidacionArchivo(true)
    }
}
