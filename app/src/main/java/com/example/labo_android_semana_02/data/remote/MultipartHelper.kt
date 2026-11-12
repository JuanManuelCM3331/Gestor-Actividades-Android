package com.example.labo_android_semana_02.data.remote

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.labo_android_semana_02.domain.model.ArchivoLocal
import com.example.labo_android_semana_02.domain.usecase.ValidarArchivoUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MultipartHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val validarArchivoUseCase = ValidarArchivoUseCase()

    fun leerYValidarUri(uri: Uri): Result<ArchivoLocal> {
        val resolver = context.contentResolver
        var nombre = "archivo"
        var tamano = -1L

        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIdx >= 0) nombre = cursor.getString(nameIdx) ?: nombre
                if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) tamano = cursor.getLong(sizeIdx)
            }
        }

        if (tamano <= 0L) {
            tamano = resolver.openInputStream(uri)?.use { input ->
                var total = 0L
                val buffer = ByteArray(8192)
                var read = input.read(buffer)
                while (read != -1) {
                    total += read
                    if (total > ValidarArchivoUseCase.MAX_FILE_SIZE_BYTES) {
                        return Result.failure(
                            IllegalArgumentException("'$nombre' supera el límite máximo de ${ValidarArchivoUseCase.MAX_FILE_SIZE_MB} MB.")
                        )
                    }
                    read = input.read(buffer)
                }
                total
            } ?: -1L
        }

        val mime = resolver.getType(uri) ?: "application/octet-stream"
        val archivo = ArchivoLocal(
            uriString = uri.toString(),
            nombre = nombre,
            mimeType = mime,
            tamanoBytes = tamano,
        )
        val validacion = validarArchivoUseCase(archivo)
        return if (validacion.esValido) {
            Result.success(archivo)
        } else {
            Result.failure(IllegalArgumentException(validacion.mensajeError ?: "Archivo inválido."))
        }
    }

    fun crearParteTexto(valor: String): RequestBody =
        valor.toRequestBody("text/plain".toMediaTypeOrNull())

    fun crearParteArchivo(fieldName: String, archivo: ArchivoLocal): Result<MultipartBody.Part> {
        val validacion = validarArchivoUseCase(archivo)
        if (!validacion.esValido) {
            return Result.failure(IllegalArgumentException(validacion.mensajeError ?: "Archivo inválido."))
        }

        return runCatching {
            val uri = Uri.parse(archivo.uriString)
            val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
                val out = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                var total = 0L
                var read = input.read(buffer)
                while (read != -1) {
                    total += read
                    if (total > ValidarArchivoUseCase.MAX_FILE_SIZE_BYTES) {
                        throw IllegalArgumentException(
                            "'${archivo.nombre}' supera el límite máximo de ${ValidarArchivoUseCase.MAX_FILE_SIZE_MB} MB."
                        )
                    }
                    out.write(buffer, 0, read)
                    read = input.read(buffer)
                }
                out.toByteArray()
            } ?: throw IllegalStateException("No se pudo abrir '${archivo.nombre}'.")

            val mediaType = archivo.mimeType.toMediaTypeOrNull() ?: "application/octet-stream".toMediaTypeOrNull()
            val body = bytes.toRequestBody(mediaType)
            MultipartBody.Part.createFormData(fieldName, archivo.nombre, body)
        }
    }
}
