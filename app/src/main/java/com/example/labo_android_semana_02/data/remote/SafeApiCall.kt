package com.example.labo_android_semana_02.data.remote

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class ApiException(
    val httpCode: Int?,
    override val message: String,
) : Exception(message)

private val errorJsonParser = Json { ignoreUnknownKeys = true }

private fun extraerMensajeError(rawBody: String?, fallbackCode: Int?): String {
    if (!rawBody.isNullOrBlank()) {
        val parsed = runCatching {
            val obj = errorJsonParser.parseToJsonElement(rawBody).jsonObject
            obj["error"]?.jsonPrimitive?.content ?: obj["message"]?.jsonPrimitive?.content
        }.getOrNull()
        if (!parsed.isNullOrBlank()) return parsed
    }
    return when (fallbackCode) {
        400 -> "Verifica los datos ingresados."
        401 -> "Tu sesión expiró o no estás autenticado."
        403 -> "No tienes permiso para realizar esta acción."
        404 -> "El recurso solicitado no existe."
        409 -> "Ya existe o no puede modificarse el registro."
        else -> "Error en el servidor (${fallbackCode ?: "desconocido"})."
    }
}

suspend fun <T> safeApiCall(block: suspend () -> T): Result<T> {
    return try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        val body = e.response()?.errorBody()?.string()
        val msg = extraerMensajeError(body, e.code())
        Result.failure(ApiException(e.code(), msg))
    } catch (e: IOException) {
        Result.failure(ApiException(null, "Sin conexión con el servidor. Verifica MySQL/API y tu red."))
    } catch (e: Exception) {
        Result.failure(ApiException(null, e.message ?: "Error inesperado."))
    }
}

suspend fun safeVoidCall(block: suspend () -> Response<Unit>): Result<Unit> {
    return try {
        val response = block()
        if (response.isSuccessful) {
            Result.success(Unit)
        } else {
            val body = response.errorBody()?.string()
            val msg = extraerMensajeError(body, response.code())
            Result.failure(ApiException(response.code(), msg))
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        Result.failure(ApiException(null, "Sin conexión con el servidor. Verifica MySQL/API y tu red."))
    } catch (e: Exception) {
        Result.failure(ApiException(null, e.message ?: "Error inesperado."))
    }
}
