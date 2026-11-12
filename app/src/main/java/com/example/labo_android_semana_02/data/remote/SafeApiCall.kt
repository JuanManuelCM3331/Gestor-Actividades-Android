package com.example.labo_android_semana_02.data.remote

import kotlinx.coroutines.CancellationException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException

class HttpStatusException(val code: Int, message: String) : Exception(message)

internal suspend inline fun <T> safeApiCall(
    crossinline block: suspend () -> Response<T>
): Result<T> {
    return try {
        val response = block()
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) {
                Result.success(body)
            } else {
                @Suppress("UNCHECKED_CAST")
                Result.success(Unit as T)
            }
        } else {
            val message = when (response.code()) {
                400 -> "Datos inválidos enviados al servidor."
                401 -> "Sesión expirada. Por favor, inicie sesión de nuevo."
                404 -> "El recurso solicitado no existe."
                in 500..599 -> "Error en el servidor MySQL. Intente más tarde."
                else -> "Error inesperado: ${response.code()}"
            }
            Result.failure(HttpStatusException(response.code(), message))
        }
    } catch (_: SocketTimeoutException) {
        Result.failure(Exception("Tiempo de espera agotado. Verifique la conexión con el servidor."))
    } catch (_: IOException) {
        Result.failure(Exception("Sin conexión con el servidor MySQL (trabajando en modo offline)."))
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        Result.failure(Exception("Error de serialización o procesamiento."))
    }
}
