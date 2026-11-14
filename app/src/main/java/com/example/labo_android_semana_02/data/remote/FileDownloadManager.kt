package com.example.labo_android_semana_02.data.remote

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.labo_android_semana_02.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
) {
    suspend fun descargarEnCache(
        urlRelativaOCompleta: String,
        nombreArchivo: String,
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val fullUrl = resolverUrl(urlRelativaOCompleta)
            val request = Request.Builder().url(fullUrl).get().build()

            val response = okHttpClient.newCall(request).execute()
            response.use { res ->
                if (!res.isSuccessful) {
                    throw IllegalStateException("No se pudo descargar el archivo (HTTP ${res.code}).")
                }
                val body = res.body ?: throw IllegalStateException("Archivo vacío en respuesta del servidor.")
                val dir = File(context.cacheDir, "descargas").apply { mkdirs() }
                val safeName = nombreArchivo.replace(Regex("[^a-zA-Z0-9._-]"), "_")
                val destino = File(dir, safeName.ifBlank { "archivo_descargado" })
                destino.outputStream().use { out ->
                    body.byteStream().copyTo(out)
                }
                destino
            }
        }
    }

    suspend fun descargarYAbrir(
        urlRelativaOCompleta: String,
        nombreArchivo: String,
        mimeType: String?,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val destino = descargarEnCache(urlRelativaOCompleta, nombreArchivo).getOrThrow()
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                destino,
            )
            val resolvedMime = mimeType?.takeIf { it.isNotBlank() } ?: "*/*"
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, resolvedMime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(viewIntent, "Abrir $nombreArchivo").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        }
    }

    fun abrirEnlaceExterno(url: String): Result<Unit> = runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun resolverUrl(url: String): String {
        if (url.startsWith("http://", ignoreCase = true) || url.startsWith("https://", ignoreCase = true)) {
            return url
        }
        val base = BuildConfig.BASE_URL.trimEnd('/')
        val cleanPath = if (url.startsWith("/")) url else "/$url"
        return "$base$cleanPath"
    }
}
