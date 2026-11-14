package com.example.labo_android_semana_02.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.labo_android_semana_02.domain.model.ArchivoAdjunto
import com.example.labo_android_semana_02.domain.model.ArchivoLocal
import com.example.labo_android_semana_02.domain.model.MensajeForo
import com.example.labo_android_semana_02.domain.model.RolUsuario
import com.example.labo_android_semana_02.ui.theme.coloresExtendidos
import java.io.File
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun esArchivoImagen(nombre: String, mime: String): Boolean {
    val ext = nombre.substringAfterLast('.', "").lowercase()
    val m = mime.lowercase()
    return m.startsWith("image/") || ext in setOf("jpg", "jpeg", "png", "webp", "gif")
}

private fun esArchivoAudio(nombre: String, mime: String): Boolean {
    val ext = nombre.substringAfterLast('.', "").lowercase()
    val m = mime.lowercase()
    return m.startsWith("audio/") || ext in setOf("mp3", "wav", "m4a", "aac", "ogg", "opus", "3gp", "amr")
}

private fun formatearSegundos(segundosTotales: Int): String {
    val mins = (segundosTotales / 60).coerceAtLeast(0)
    val segs = (segundosTotales % 60).coerceAtLeast(0)
    return String.format(Locale.US, "%02d:%02d", mins, segs)
}

@Composable
fun BurbujaMensajeChat(
    mensaje: MensajeForo,
    esMio: Boolean,
    onAbrirAdjunto: (ArchivoAdjunto) -> Unit,
    onAbrirEnlace: (String) -> Unit,
    onDescargarEnCache: suspend (ArchivoAdjunto) -> Result<File>,
    modifier: Modifier = Modifier,
) {
    val esInstructor = mensaje.autorRol == RolUsuario.INSTRUCTOR
    val acentoAutor = if (esInstructor) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.coloresExtendidos.especial
    }

    val colorContenedor = if (esMio) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.coloresExtendidos.superficieTarjeta
    }
    val colorTexto = if (esMio) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val colorBorde = if (esMio) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
    } else {
        acentoAutor.copy(alpha = 0.30f)
    }
    val formaBurbuja = if (esMio) {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp)
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (esMio) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
    ) {
        if (!esMio) {
            IconoTintado(
                icono = if (esInstructor) Icons.Default.School else Icons.Default.Person,
                color = acentoAutor,
                tamano = 36.dp,
                circular = true,
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Surface(
            color = colorContenedor,
            contentColor = colorTexto,
            shape = formaBurbuja,
            border = BorderStroke(1.dp, colorBorde),
            tonalElevation = if (esMio) 2.dp else 1.dp,
            modifier = Modifier.widthIn(min = 120.dp, max = 330.dp),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = if (esInstructor) Icons.Default.School else Icons.Default.Person,
                        contentDescription = null,
                        tint = if (esMio) MaterialTheme.colorScheme.primary else acentoAutor,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = if (esMio) "Tú (${mensaje.autorNombre})" else mensaje.autorNombre,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (esMio) MaterialTheme.colorScheme.primary else acentoAutor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Surface(
                        color = acentoAutor.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            text = if (esInstructor) "Instructor" else "Aprendiz",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (esMio) colorTexto else acentoAutor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }

                if (mensaje.contenido.isNotBlank()) {
                    Text(
                        text = mensaje.contenido,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorTexto,
                    )
                }

                if (!mensaje.enlaceUrl.isNullOrBlank()) {
                    TarjetaEnlaceChat(
                        url = mensaje.enlaceUrl,
                        onClick = { onAbrirEnlace(mensaje.enlaceUrl) },
                    )
                }

                mensaje.archivos.forEach { adj ->
                    when {
                        esArchivoImagen(adj.nombreOriginal, adj.mimeType) -> {
                            TarjetaImagenChat(
                                adjunto = adj,
                                onDescargarEnCache = onDescargarEnCache,
                                onAbrirExterno = { onAbrirAdjunto(adj) },
                            )
                        }
                        esArchivoAudio(adj.nombreOriginal, adj.mimeType) -> {
                            ReproductorAudioChat(
                                adjunto = adj,
                                onDescargarEnCache = onDescargarEnCache,
                                onAbrirExterno = { onAbrirAdjunto(adj) },
                            )
                        }
                        else -> {
                            TarjetaArchivoChat(
                                adjunto = adj,
                                onClick = { onAbrirAdjunto(adj) },
                            )
                        }
                    }
                }

                Text(
                    text = UiFormatters.formatearFechaHora(mensaje.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = colorTexto.copy(alpha = 0.72f),
                    modifier = Modifier.align(Alignment.End),
                )
            }
        }
    }
}

@Composable
private fun TarjetaEnlaceChat(
    url: String,
    onClick: () -> Unit,
) {
    val c = MaterialTheme.coloresExtendidos
    Surface(
        color = c.informacionContenedor.copy(alpha = 0.85f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, c.informacion.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Link,
                contentDescription = null,
                tint = c.informacion,
                modifier = Modifier.size(20.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Enlace compartido",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = c.informacion,
                )
                Text(
                    text = url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textDecoration = TextDecoration.Underline,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = "Abrir enlace",
                tint = c.informacion,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun TarjetaImagenChat(
    adjunto: ArchivoAdjunto,
    onDescargarEnCache: suspend (ArchivoAdjunto) -> Result<File>,
    onAbrirExterno: () -> Unit,
) {
    var bitmap by remember(adjunto.id) { mutableStateOf<ImageBitmap?>(null) }
    var cargando by remember(adjunto.id) { mutableStateOf(true) }

    LaunchedEffect(adjunto.id) {
        cargando = true
        val res = onDescargarEnCache(adjunto)
        val decoded = withContext(Dispatchers.IO) {
            res.getOrNull()?.let { file ->
                runCatching { BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap() }.getOrNull()
            }
        }
        bitmap = decoded
        cargando = false
    }

    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.coloresExtendidos.exito.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onAbrirExterno),
    ) {
        Column {
            val bmp = bitmap
            if (bmp != null) {
                Image(
                    bitmap = bmp,
                    contentDescription = adjunto.nombreOriginal,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = 210.dp)
                        .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)),
                )
            } else if (cargando) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(26.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.coloresExtendidos.exito,
                    )
                }
            }
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    tint = MaterialTheme.coloresExtendidos.exito,
                    modifier = Modifier.size(18.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = adjunto.nombreOriginal,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "Imagen • ${UiFormatters.formatearTamano(adjunto.tamanoBytes)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = "Ver imagen",
                    tint = MaterialTheme.coloresExtendidos.exito,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun ReproductorAudioChat(
    adjunto: ArchivoAdjunto,
    onDescargarEnCache: suspend (ArchivoAdjunto) -> Result<File>,
    onAbrirExterno: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val c = MaterialTheme.coloresExtendidos
    var reproduciendo by remember { mutableStateOf(false) }
    var descargando by remember { mutableStateOf(false) }
    var posicionSeg by remember { mutableIntStateOf(0) }
    var duracionSeg by remember { mutableIntStateOf(0) }
    val playerRef = remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(adjunto.id) {
        onDispose {
            runCatching {
                playerRef.value?.stop()
                playerRef.value?.release()
            }
            playerRef.value = null
        }
    }

    LaunchedEffect(reproduciendo) {
        while (isActive && reproduciendo) {
            val mp = playerRef.value
            if (mp != null && runCatching { mp.isPlaying }.getOrDefault(false)) {
                posicionSeg = (mp.currentPosition / 1000).coerceAtLeast(0)
                duracionSeg = (mp.duration / 1000).coerceAtLeast(1)
            }
            delay(250L)
        }
    }

    Surface(
        color = c.especialContenedor.copy(alpha = 0.85f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, c.especial.copy(alpha = 0.40f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(
                color = c.especial,
                shape = CircleShape,
                modifier = Modifier.size(38.dp),
            ) {
                AppIconButton(
                    onClick = {
                        val actual = playerRef.value
                        if (actual != null) {
                            if (reproduciendo) {
                                runCatching { actual.pause() }
                                reproduciendo = false
                            } else {
                                runCatching { actual.start() }
                                reproduciendo = true
                            }
                        } else if (!descargando) {
                            scope.launch {
                                descargando = true
                                val res = onDescargarEnCache(adjunto)
                                descargando = false
                                res.onSuccess { file ->
                                    runCatching {
                                        val mp = MediaPlayer().apply {
                                            setDataSource(file.absolutePath)
                                            prepare()
                                            duracionSeg = (duration / 1000).coerceAtLeast(1)
                                            setOnCompletionListener {
                                                reproduciendo = false
                                                posicionSeg = 0
                                            }
                                            start()
                                        }
                                        playerRef.value = mp
                                        reproduciendo = true
                                    }.onFailure {
                                        onAbrirExterno()
                                    }
                                }.onFailure {
                                    onAbrirExterno()
                                }
                            }
                        }
                    },
                ) {
                    if (descargando) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Icon(
                            imageVector = if (reproduciendo) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (reproduciendo) "Pausar audio" else "Reproducir audio",
                            tint = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = c.especial,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = adjunto.nombreOriginal,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                val progreso = if (duracionSeg > 0) {
                    (posicionSeg.toFloat() / duracionSeg.toFloat()).coerceIn(0f, 1f)
                } else {
                    0f
                }
                LinearProgressIndicator(
                    progress = { progreso },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = c.especial,
                    trackColor = c.especial.copy(alpha = 0.22f),
                )
                Text(
                    text = if (duracionSeg > 0) {
                        "${formatearSegundos(posicionSeg)} / ${formatearSegundos(duracionSeg)} • ${UiFormatters.formatearTamano(adjunto.tamanoBytes)}"
                    } else {
                        "Audio • ${UiFormatters.formatearTamano(adjunto.tamanoBytes)}"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            AppIconButton(onClick = onAbrirExterno) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Abrir o guardar audio",
                    tint = c.especial,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun TarjetaArchivoChat(
    adjunto: ArchivoAdjunto,
    onClick: () -> Unit,
) {
    val colorTipo = colorParaTipoArchivo(adjunto.nombreOriginal, adjunto.mimeType)
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, colorTipo.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconoTintado(
                icono = Icons.Default.Description,
                color = colorTipo,
                tamano = 34.dp,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = adjunto.nombreOriginal,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = UiFormatters.formatearTamano(adjunto.tamanoBytes),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = "Descargar archivo",
                tint = colorTipo,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
fun BarraCompositorChat(
    texto: String,
    onTextoChange: (String) -> Unit,
    enlaceUrl: String,
    mostrarCampoEnlace: Boolean,
    onEnlaceChange: (String) -> Unit,
    onAlternarCampoEnlace: () -> Unit,
    archivosPendientes: List<ArchivoLocal>,
    onAgregarUri: (Uri) -> Unit,
    onAgregarAudioGrabado: (File) -> Unit,
    onQuitarArchivo: (Int) -> Unit,
    onEnviar: () -> Unit,
    onErrorPermisoAudio: (String) -> Unit,
    enviando: Boolean,
    habilitado: Boolean = true,
    placeholder: String = "Escribe un mensaje...",
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val c = MaterialTheme.coloresExtendidos

    var grabandoAudio by remember { mutableStateOf(false) }
    var segundosGrabacion by remember { mutableIntStateOf(0) }
    val recorderRef = remember { mutableStateOf<MediaRecorder?>(null) }
    val archivoGrabacionRef = remember { mutableStateOf<File?>(null) }

    fun detenerGrabacion(guardar: Boolean) {
        val rec = recorderRef.value
        val file = archivoGrabacionRef.value
        recorderRef.value = null
        archivoGrabacionRef.value = null
        grabandoAudio = false
        segundosGrabacion = 0
        if (rec != null) {
            val detenidoOk = runCatching {
                rec.stop()
                true
            }.getOrDefault(false)
            runCatching { rec.release() }
            if (guardar && detenidoOk && file != null && file.exists() && file.length() > 0L) {
                onAgregarAudioGrabado(file)
            } else {
                file?.delete()
            }
        }
    }

    fun iniciarGrabacion() {
        runCatching {
            val dir = File(context.cacheDir, "grabaciones_foro").apply { mkdirs() }
            val out = File(dir, "nota_voz_${System.currentTimeMillis()}.m4a")
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(96_000)
                setAudioSamplingRate(44_100)
                setOutputFile(out.absolutePath)
                prepare()
                start()
            }
            recorderRef.value = recorder
            archivoGrabacionRef.value = out
            segundosGrabacion = 0
            grabandoAudio = true
        }.onFailure { e ->
            detenerGrabacion(guardar = false)
            onErrorPermisoAudio("No se pudo iniciar el micrófono: ${e.message ?: "error de hardware"}.")
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            detenerGrabacion(guardar = false)
        }
    }

    LaunchedEffect(grabandoAudio) {
        while (isActive && grabandoAudio) {
            delay(1000L)
            segundosGrabacion += 1
        }
    }

    val permisoMicLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { concedido ->
        if (concedido) {
            iniciarGrabacion()
        } else {
            onErrorPermisoAudio("Se requiere permiso de micrófono para grabar notas de voz.")
        }
    }

    val selectorImagenesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        uris.forEach(onAgregarUri)
    }

    val selectorAudiosLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        uris.forEach(onAgregarUri)
    }

    val selectorArchivosLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        uris.forEach(onAgregarUri)
    }

    val urlValidaNoVacia = mostrarCampoEnlace && enlaceUrl.trim().isNotEmpty() && enlaceUrl.trim() != "https://"
    val puedeEnviar = habilitado && !enviando && !grabandoAudio &&
        (texto.isNotBlank() || urlValidaNoVacia || archivosPendientes.isNotEmpty())

    Surface(
        color = c.superficieTarjeta,
        tonalElevation = 3.dp,
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // 1. Indicador en vivo cuando está grabando nota de voz
            if (grabandoAudio) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = "Grabando audio ${formatearSegundos(segundosGrabacion)}...",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f),
                        )
                        AppTextButton(
                            onClick = { detenerGrabacion(guardar = false) },
                            icon = Icons.Default.Delete,
                        ) {
                            Text("Cancelar", color = MaterialTheme.colorScheme.error)
                        }
                        AppButton(
                            onClick = { detenerGrabacion(guardar = true) },
                            icon = Icons.Default.StopCircle,
                        ) {
                            Text("Listo")
                        }
                    }
                }
            }

            // 2. Campo compacto para compartir URL (sin abrir modal)
            if (mostrarCampoEnlace) {
                OutlinedTextField(
                    value = enlaceUrl,
                    onValueChange = onEnlaceChange,
                    label = { Text("Enlace URL (https://...)") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = null,
                            tint = c.informacion,
                        )
                    },
                    trailingIcon = {
                        AppIconButton(onClick = onAlternarCampoEnlace) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Quitar enlace",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // 3. Vista previa de archivos, imágenes o audios adjuntos pendientes de envío
            if (archivosPendientes.isNotEmpty()) {
                WrapRow(horizontalSpacing = 6.dp, verticalSpacing = 6.dp) {
                    archivosPendientes.forEachIndexed { idx, arch ->
                        val esImg = esArchivoImagen(arch.nombre, arch.mimeType)
                        val esAud = esArchivoAudio(arch.nombre, arch.mimeType)
                        val colorChip = when {
                            esImg -> c.exito
                            esAud -> c.especial
                            else -> colorParaTipoArchivo(arch.nombre, arch.mimeType)
                        }
                        val iconoChip = when {
                            esImg -> Icons.Default.Image
                            esAud -> Icons.Default.AudioFile
                            else -> Icons.Default.Description
                        }
                        Surface(
                            color = colorChip.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, colorChip.copy(alpha = 0.35f)),
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    imageVector = iconoChip,
                                    contentDescription = null,
                                    tint = colorChip,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    text = "${arch.nombre} (${UiFormatters.formatearTamano(arch.tamanoBytes)})",
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 190.dp),
                                )
                                AppIconButton(
                                    onClick = { onQuitarArchivo(idx) },
                                    modifier = Modifier.size(22.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Quitar adjunto",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(14.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Barra de herramientas multimedia (Imagen, Archivo, Audio, Grabar voz, Enlace URL)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BotonAccionMultimedia(
                        icono = Icons.Default.Image,
                        etiqueta = "Imagen",
                        color = c.exito,
                        enabled = habilitado && !enviando,
                        onClick = { selectorImagenesLauncher.launch(arrayOf("image/*")) },
                    )
                    BotonAccionMultimedia(
                        icono = Icons.Default.AttachFile,
                        etiqueta = "Archivo",
                        color = MaterialTheme.colorScheme.primary,
                        enabled = habilitado && !enviando,
                        onClick = { selectorArchivosLauncher.launch(arrayOf("*/*")) },
                    )
                    BotonAccionMultimedia(
                        icono = Icons.Default.AudioFile,
                        etiqueta = "Audio",
                        color = c.especial,
                        enabled = habilitado && !enviando,
                        onClick = { selectorAudiosLauncher.launch(arrayOf("audio/*")) },
                    )
                    BotonAccionMultimedia(
                        icono = if (grabandoAudio) Icons.Default.StopCircle else Icons.Default.Mic,
                        etiqueta = if (grabandoAudio) "Parar" else "Voz",
                        color = MaterialTheme.colorScheme.error,
                        enabled = habilitado && !enviando,
                        onClick = {
                            if (grabandoAudio) {
                                detenerGrabacion(guardar = true)
                            } else {
                                val tienePermiso = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO,
                                ) == PackageManager.PERMISSION_GRANTED
                                if (tienePermiso) {
                                    iniciarGrabacion()
                                } else {
                                    permisoMicLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                    )
                    BotonAccionMultimedia(
                        icono = Icons.Default.Link,
                        etiqueta = "URL",
                        color = c.informacion,
                        enabled = habilitado && !enviando,
                        activo = mostrarCampoEnlace,
                        onClick = onAlternarCampoEnlace,
                    )
                }
            }

            // 5. Barra principal de escritura y botón de envío directo
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = texto,
                    onValueChange = onTextoChange,
                    enabled = habilitado && !enviando,
                    placeholder = { Text(placeholder) },
                    maxLines = 4,
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.weight(1f),
                )
                Surface(
                    color = if (puedeEnviar) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant,
                    shape = CircleShape,
                    modifier = Modifier.size(48.dp),
                ) {
                    AppIconButton(
                        onClick = onEnviar,
                        enabled = puedeEnviar,
                    ) {
                        if (enviando) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Enviar mensaje",
                                tint = if (puedeEnviar) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BotonAccionMultimedia(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    etiqueta: String,
    color: androidx.compose.ui.graphics.Color,
    enabled: Boolean,
    activo: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        color = if (activo) color.copy(alpha = 0.24f) else color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(10.dp),
        border = if (activo) BorderStroke(1.dp, color) else null,
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = icono,
                contentDescription = etiqueta,
                tint = if (enabled) color else color.copy(alpha = 0.4f),
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = etiqueta,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) color else color.copy(alpha = 0.4f),
            )
        }
    }
}
