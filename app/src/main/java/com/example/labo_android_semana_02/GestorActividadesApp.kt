package com.example.labo_android_semana_02

import android.Manifest
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.HiltAndroidApp
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.atomic.AtomicInteger

@HiltAndroidApp
class GestorActividadesApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificadorLocal.inicializarCanal(this)
        instalarManejadorFallosLocal()
    }

    /**
     * Registra en disco local (filesDir/crash_logs) cualquier excepción no capturada
     * sin depender de servicios externos.
     */
    private fun instalarManejadorFallosLocal() {
        val handlerPrevio = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                val dir = File(filesDir, "crash_logs").apply { mkdirs() }
                val archivo = File(dir, "ultimo_crash.log")
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                val reporte = buildString {
                    appendLine("Timestamp: ${System.currentTimeMillis()}")
                    appendLine("Thread: ${thread.name}")
                    appendLine("Message: ${throwable.message}")
                    appendLine(sw.toString())
                }
                archivo.writeText(reporte)
            }
            handlerPrevio?.uncaughtException(thread, throwable)
        }
    }
}

object NotificadorLocal {
    private const val CHANNEL_ID = "ctma_eventos_academicos"
    private val nextId = AtomicInteger(1000)

    fun inicializarCanal(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Eventos Académicos CTMA",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Notificaciones de nuevos mensajes en el foro, actividades y calificaciones"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun mostrarNotificacion(context: Context, titulo: String, contenido: String) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        runCatching {
            val notif = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(titulo)
                .setContentText(contenido)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(context).notify(nextId.incrementAndGet(), notif)
        }
    }
}
