package com.example.labo_android_semana_02.ui.screens

import com.example.labo_android_semana_02.ui.components.AppAssistChip
import com.example.labo_android_semana_02.ui.components.AppButton
import com.example.labo_android_semana_02.ui.components.AppFilterChip
import com.example.labo_android_semana_02.ui.components.AppIconButton
import com.example.labo_android_semana_02.ui.components.AppOutlinedButton

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.labo_android_semana_02.ui.components.BarraCompositorChat
import com.example.labo_android_semana_02.ui.components.BurbujaMensajeChat
import com.example.labo_android_semana_02.ui.components.IconoTintado
import com.example.labo_android_semana_02.ui.components.TituloConIcono
import com.example.labo_android_semana_02.ui.components.colorParaTipoArchivo
import com.example.labo_android_semana_02.ui.theme.coloresExtendidos
import com.example.labo_android_semana_02.domain.model.Actividad
import com.example.labo_android_semana_02.domain.model.ArchivoAdjunto
import com.example.labo_android_semana_02.domain.model.EstadoEntrega
import com.example.labo_android_semana_02.domain.model.MaterialApoyo
import com.example.labo_android_semana_02.domain.model.PersonaCurso
import com.example.labo_android_semana_02.domain.model.RolUsuario
import com.example.labo_android_semana_02.domain.model.TipoMaterial
import com.example.labo_android_semana_02.ui.components.DialogoConfirmacion
import com.example.labo_android_semana_02.ui.components.EstadoEntregaChip
import com.example.labo_android_semana_02.ui.components.EstadoVacioTarjeta
import com.example.labo_android_semana_02.ui.components.UiFormatters
import com.example.labo_android_semana_02.ui.components.WrapRow
import com.example.labo_android_semana_02.ui.viewmodel.CursoPanelUiState
import java.io.File

@Composable
fun TabActividadesContenido(
    state: CursoPanelUiState,
    esInstructor: Boolean,
    onNuevaActividad: () -> Unit,
    onEditarActividad: (Actividad) -> Unit,
    onEliminarActividad: (Int) -> Unit,
    onAbrirEntregasInstructor: (Int) -> Unit,
    onAbrirEntregaAprendiz: (Int) -> Unit,
    onAbrirAdjunto: (ArchivoAdjunto) -> Unit,
) {
    var actividadAEliminar by remember { mutableStateOf<Actividad?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (esInstructor) {
            AppButton(
                onClick = onNuevaActividad,
                icon = Icons.Default.Add,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Publicar nueva actividad")
            }
        }

        if (state.actividades.isEmpty() && !state.cargando) {
            EstadoVacioTarjeta(
                icono = Icons.AutoMirrored.Filled.Assignment,
                titulo = "Sin actividades publicadas",
                descripcion = if (esInstructor) {
                    "Crea la primera actividad con fecha límite, puntaje máximo y adjuntos."
                } else {
                    "Tu instructor aún no ha publicado actividades en este curso."
                },
            )
        }

        state.actividades.forEach { act ->
            val acento = colorEstadoActividad(act.estadoAprendiz, esInstructor)
            Card(
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp, pressedElevation = 0.dp),
                border = BorderStroke(1.dp, acento.copy(alpha = 0.30f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (esInstructor) onAbrirEntregasInstructor(act.id)
                        else onAbrirEntregaAprendiz(act.id)
                    },
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconoTintado(
                            icono = Icons.AutoMirrored.Filled.Assignment,
                            color = acento,
                            tamano = 42.dp,
                        )
                        Text(
                            text = act.titulo,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        if (!esInstructor && act.estadoAprendiz != null) {
                            EstadoEntregaChip(estado = act.estadoAprendiz)
                        }
                    }

                    if (act.descripcion.isNotBlank()) {
                        Text(
                            text = act.descripcion,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.coloresExtendidos.informacion,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = " ${UiFormatters.formatearFechaHora(act.fechaLimite)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(
                            imageVector = Icons.Default.Grade,
                            contentDescription = null,
                            tint = MaterialTheme.coloresExtendidos.advertencia,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = " Máx. ${UiFormatters.formatearNota(act.puntajeMaximo)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    if (act.adjuntos.isNotEmpty()) {
                        WrapRow(
                            horizontalSpacing = 6.dp,
                            verticalSpacing = 6.dp,
                        ) {
                            act.adjuntos.forEach { adj ->
                                AppAssistChip(
                                    onClick = { onAbrirAdjunto(adj) },
                                    icon = Icons.Default.AttachFile,
                                    label = {
                                        Text("${adj.nombreOriginal} (${UiFormatters.formatearTamano(adj.tamanoBytes)})")
                                    },
                                )
                            }
                        }
                    }

                    if (esInstructor) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AppOutlinedButton(
                                onClick = { onAbrirEntregasInstructor(act.id) },
                                icon = Icons.Default.Inbox,
                            ) {
                                Text("Entregas (${act.totalEntregas}) • Por calificar: ${act.porCalificar}")
                            }
                            Row {
                                AppIconButton(onClick = { onEditarActividad(act) }) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Editar",
                                        tint = MaterialTheme.coloresExtendidos.informacion,
                                    )
                                }
                                AppIconButton(onClick = { actividadAEliminar = act }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Eliminar",
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                        }
                    } else {
                        val entrega = act.miEntrega
                        if (entrega != null && entrega.estado == EstadoEntrega.CALIFICADA && entrega.nota != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Grade,
                                    contentDescription = null,
                                    tint = MaterialTheme.coloresExtendidos.exito,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    text = " Calificación: ${UiFormatters.formatearNota(entrega.nota)} / ${UiFormatters.formatearNota(act.puntajeMaximo)}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.coloresExtendidos.exito,
                                )
                            }
                        }
                        val calificadaOExenta = act.estadoAprendiz == EstadoEntrega.CALIFICADA ||
                            act.estadoAprendiz == EstadoEntrega.EXENTA
                        AppOutlinedButton(
                            onClick = { onAbrirEntregaAprendiz(act.id) },
                            icon = when {
                                calificadaOExenta -> Icons.Default.Grade
                                act.estadoAprendiz == EstadoEntrega.ENTREGADA -> Icons.Default.Edit
                                else -> Icons.Default.Upload
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            val textoBtn = when (act.estadoAprendiz) {
                                EstadoEntrega.CALIFICADA, EstadoEntrega.EXENTA -> "Ver calificación y retroalimentación"
                                EstadoEntrega.ENTREGADA -> "Ver o editar entrega"
                                else -> "Entregar evidencia"
                            }
                            Text(textoBtn)
                        }
                    }
                }
            }
        }
    }

    actividadAEliminar?.let { act ->
        DialogoConfirmacion(
            titulo = "Eliminar actividad",
            mensaje = "¿Deseas eliminar '${act.titulo}' junto con todas sus entregas?",
            textoConfirmar = "Eliminar",
            onConfirmar = {
                onEliminarActividad(act.id)
                actividadAEliminar = null
            },
            onCancelar = { actividadAEliminar = null },
        )
    }
}

@Composable
fun TabMaterialContenido(
    state: CursoPanelUiState,
    esInstructor: Boolean,
    onNuevoMaterial: () -> Unit,
    onEliminarMaterial: (Int) -> Unit,
    onAbrirMaterial: (MaterialApoyo) -> Unit,
) {
    var materialAEliminar by remember { mutableStateOf<MaterialApoyo?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (esInstructor) {
            AppButton(onClick = onNuevoMaterial, icon = Icons.Default.Upload, modifier = Modifier.fillMaxWidth()) {
                Text("Subir archivo o enlace de apoyo")
            }
        }

        if (state.materiales.isEmpty() && !state.cargando) {
            EstadoVacioTarjeta(
                icono = Icons.Default.Description,
                titulo = "Sin material de apoyo",
                descripcion = if (esInstructor) {
                    "Comparte guías PDF, presentaciones, videos o enlaces de referencia."
                } else {
                    "Tu instructor aún no ha compartido material de apoyo."
                },
            )
        }

        state.materiales.forEach { mat ->
            val esEnlace = mat.tipo == TipoMaterial.ENLACE
            val colorMaterial = if (esEnlace) MaterialTheme.coloresExtendidos.informacion
            else colorParaTipoArchivo(mat.nombreOriginal.orEmpty())
            Card(
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, colorMaterial.copy(alpha = 0.30f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconoTintado(
                            icono = if (esEnlace) Icons.Default.Link else Icons.Default.Description,
                            color = colorMaterial,
                            tamano = 42.dp,
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = mat.titulo,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = if (esEnlace) {
                                    "Enlace externo"
                                } else {
                                    "Archivo: ${mat.nombreOriginal ?: ""} (${UiFormatters.formatearTamano(mat.tamanoBytes)})"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = colorMaterial,
                            )
                        }
                        if (esInstructor) {
                            AppIconButton(onClick = { materialAEliminar = mat }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Eliminar material",
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }

                    if (mat.descripcion.isNotBlank()) {
                        Text(
                            text = mat.descripcion,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    AppOutlinedButton(
                        onClick = { onAbrirMaterial(mat) },
                        icon = if (esEnlace) Icons.AutoMirrored.Filled.OpenInNew else Icons.Default.Download,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (esEnlace) "Abrir enlace" else "Descargar y abrir")
                    }
                }
            }
        }
    }

    materialAEliminar?.let { mat ->
        DialogoConfirmacion(
            titulo = "Eliminar material",
            mensaje = "¿Deseas eliminar '${mat.titulo}'?",
            textoConfirmar = "Eliminar",
            onConfirmar = {
                onEliminarMaterial(mat.id)
                materialAEliminar = null
            },
            onCancelar = { materialAEliminar = null },
        )
    }
}

@Composable
fun TabForoContenido(
    state: CursoPanelUiState,
    esInstructor: Boolean,
    onCambiarModoForo: (Boolean) -> Unit,
    onCambiarFiltroAprendiz: (Int?) -> Unit,
    onChatTextoChange: (String) -> Unit,
    onChatEnlaceChange: (String) -> Unit,
    onAlternarCampoEnlaceChat: () -> Unit,
    onAgregarArchivoChatUri: (Uri) -> Unit,
    onAgregarAudioGrabadoChat: (File) -> Unit,
    onQuitarArchivoChat: (Int) -> Unit,
    onEnviarMensajeChat: () -> Unit,
    onAbrirAdjunto: (ArchivoAdjunto) -> Unit,
    onAbrirEnlace: (String) -> Unit,
    onDescargarEnCache: suspend (ArchivoAdjunto) -> Result<File>,
    onMostrarError: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val mensajes = if (state.modoForoPrivado) state.mensajesChatPrivado else state.mensajesChatPublico
    val aprendices = state.personas?.aprendices.orEmpty()
    val aprendizSeleccionado = aprendices.firstOrNull { it.id == state.filtroAprendizForoId }
    val listState = rememberLazyListState()

    LaunchedEffect(mensajes.size, state.modoForoPrivado, state.filtroAprendizForoId) {
        if (mensajes.isNotEmpty()) {
            listState.animateScrollToItem(mensajes.lastIndex)
        }
    }

    val puedeChatear = !state.modoForoPrivado || !esInstructor || state.filtroAprendizForoId != null

    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AppFilterChip(
                    selected = !state.modoForoPrivado,
                    onClick = { onCambiarModoForo(false) },
                    icon = Icons.Default.Public,
                    label = { Text("Chat grupal", maxLines = 1) },
                    modifier = Modifier.weight(1f),
                )
                AppFilterChip(
                    selected = state.modoForoPrivado,
                    onClick = { onCambiarModoForo(true) },
                    icon = Icons.Default.Lock,
                    label = { Text("Chat privado", maxLines = 1) },
                    modifier = Modifier.weight(1f),
                )
            }

            if (state.modoForoPrivado) {
                if (esInstructor) {
                    if (aprendices.isNotEmpty()) {
                        Text(
                            text = "Conversación privada con aprendiz:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        WrapRow(horizontalSpacing = 6.dp, verticalSpacing = 6.dp) {
                            aprendices.forEach { est ->
                                AppFilterChip(
                                    selected = state.filtroAprendizForoId == est.id,
                                    onClick = { onCambiarFiltroAprendiz(est.id) },
                                    icon = Icons.Default.Person,
                                    label = { Text(est.nombre) },
                                )
                            }
                        }
                    }
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = "Chat privado 1 a 1 con ${state.curso?.instructorNombre ?: "tu instructor"}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                        }
                    }
                }
            }
        }

        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            if (state.modoForoPrivado && esInstructor && aprendices.isEmpty() && !state.cargando) {
                item {
                    EstadoVacioTarjeta(
                        icono = Icons.Default.Lock,
                        titulo = "Sin aprendices inscritos",
                        descripcion = "Cuando los aprendices se unan al curso con el código de acceso podrás chatear 1 a 1 con cada uno.",
                    )
                }
            } else if (mensajes.isEmpty() && !state.cargando) {
                item {
                    EstadoVacioTarjeta(
                        icono = if (state.modoForoPrivado) Icons.Default.Lock else Icons.Default.Public,
                        titulo = if (state.modoForoPrivado) {
                            if (esInstructor && aprendizSeleccionado != null) {
                                "Inicia el chat con ${aprendizSeleccionado.nombre}"
                            } else {
                                "Sin mensajes privados aún"
                            }
                        } else {
                            "El chat grupal está listo"
                        },
                        descripcion = "Escribe en la barra inferior o adjunta imágenes, archivos, notas de voz, audios o enlaces URL.",
                    )
                }
            }

            items(mensajes, key = { it.id }) { msg ->
                BurbujaMensajeChat(
                    mensaje = msg,
                    esMio = msg.autorId == state.usuario?.id,
                    onAbrirAdjunto = onAbrirAdjunto,
                    onAbrirEnlace = onAbrirEnlace,
                    onDescargarEnCache = onDescargarEnCache,
                )
            }
        }

        BarraCompositorChat(
            texto = state.chatTexto,
            onTextoChange = onChatTextoChange,
            enlaceUrl = state.chatEnlaceUrl,
            mostrarCampoEnlace = state.mostrarCampoEnlaceChat,
            onEnlaceChange = onChatEnlaceChange,
            onAlternarCampoEnlace = onAlternarCampoEnlaceChat,
            archivosPendientes = state.chatArchivosPendientes,
            onAgregarUri = onAgregarArchivoChatUri,
            onAgregarAudioGrabado = onAgregarAudioGrabadoChat,
            onQuitarArchivo = onQuitarArchivoChat,
            onEnviar = onEnviarMensajeChat,
            onErrorPermisoAudio = onMostrarError,
            enviando = state.enviandoMensajeChat,
            habilitado = puedeChatear,
            placeholder = when {
                !puedeChatear -> "Selecciona un aprendiz para chatear..."
                state.modoForoPrivado && esInstructor && aprendizSeleccionado != null ->
                    "Mensaje privado para ${aprendizSeleccionado.nombre}..."
                state.modoForoPrivado -> "Escribe un mensaje privado al instructor..."
                else -> "Escribe un mensaje para todo el curso..."
            },
        )
    }
}

@Composable
fun TabPersonasContenido(
    state: CursoPanelUiState,
    esInstructor: Boolean,
    onExpulsarEstudiante: (Int) -> Unit,
    onSalirseDelCurso: () -> Unit,
) {
    var estudianteAExpulsar by remember { mutableStateOf<PersonaCurso?>(null) }
    var confirmarSalida by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        state.personas?.instructor?.let { inst ->
            Card(
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconoTintado(
                        icono = Icons.Default.School,
                        color = MaterialTheme.colorScheme.primary,
                        tamano = 44.dp,
                        circular = true,
                    )
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(inst.nombre, fontWeight = FontWeight.Bold)
                        Text("Instructor • ${inst.email}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        val aprendices = state.personas?.aprendices.orEmpty()
        TituloConIcono(
            texto = "Aprendices inscritos (${aprendices.size})",
            icono = Icons.Default.Person,
            color = MaterialTheme.coloresExtendidos.especial,
        )

        if (aprendices.isEmpty() && !state.cargando) {
            EstadoVacioTarjeta(
                icono = Icons.Default.Person,
                titulo = "Sin aprendices inscritos",
                descripcion = "Comparte el código de acceso para que los aprendices ingresen al curso.",
            )
        }

        aprendices.forEach { est ->
            Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconoTintado(
                        icono = Icons.Default.Person,
                        color = MaterialTheme.coloresExtendidos.especial,
                        tamano = 40.dp,
                        circular = true,
                    )
                    Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(est.nombre, fontWeight = FontWeight.Medium)
                        Text(
                            text = est.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (est.nota05 != null && est.nota0100 != null) {
                            Text(
                                text = "Promedio: ${UiFormatters.formatearNota(est.nota05)} / 5.0  (${UiFormatters.formatearNota(est.nota0100)} / 100)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.coloresExtendidos.exito,
                            )
                        }
                    }
                    if (esInstructor) {
                        AppOutlinedButton(onClick = { estudianteAExpulsar = est }, icon = Icons.Default.PersonRemove) {
                            Text("Expulsar", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        if (!esInstructor) {
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            AppOutlinedButton(
                onClick = { confirmarSalida = true },
                icon = Icons.AutoMirrored.Filled.ExitToApp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Salirse de este curso", color = MaterialTheme.colorScheme.error)
            }
        }
    }

    estudianteAExpulsar?.let { est ->
        DialogoConfirmacion(
            titulo = "Expulsar aprendiz",
            mensaje = "¿Seguro que deseas expulsar a ${est.nombre} del curso?",
            textoConfirmar = "Expulsar",
            onConfirmar = {
                onExpulsarEstudiante(est.id)
                estudianteAExpulsar = null
            },
            onCancelar = { estudianteAExpulsar = null },
        )
    }

    if (confirmarSalida) {
        DialogoConfirmacion(
            titulo = "Salirse del curso",
            mensaje = "¿Deseas abandonar este curso?",
            textoConfirmar = "Salirse",
            onConfirmar = {
                confirmarSalida = false
                onSalirseDelCurso()
            },
            onCancelar = { confirmarSalida = false },
        )
    }
}

@Composable
fun TabNotasContenido(
    state: CursoPanelUiState,
    onExportarExcel: () -> Unit,
) {
    val c = MaterialTheme.coloresExtendidos
    val calificaciones = if (state.calificacionesCurso.isNotEmpty()) {
        state.calificacionesCurso
    } else {
        listOfNotNull(state.miCalificacion)
    }
    val actividades = state.actividades

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TituloConIcono(
                texto = "Calificaciones (${calificaciones.size})",
                icono = Icons.Default.Grade,
                color = c.exito,
                modifier = Modifier.weight(1f),
            )
            AppButton(
                onClick = onExportarExcel,
                enabled = calificaciones.isNotEmpty() && !state.procesandoArchivo,
                icon = Icons.Default.TableChart,
            ) {
                Text("Exportar Excel")
            }
        }

        if (calificaciones.isEmpty() && !state.cargando) {
            EstadoVacioTarjeta(
                icono = Icons.Default.Grade,
                titulo = "Sin calificaciones aún",
                descripcion = "Cuando haya aprendices inscritos en el curso, aquí aparecerá la tabla completa de notas por actividad y el promedio total.",
            )
        } else if (calificaciones.isNotEmpty()) {
            val scrollHorizontal = rememberScrollState()
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = c.superficieTarjeta),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollHorizontal),
                ) {
                    // Encabezado de la tabla
                    Row(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CeldaEncabezadoTabla(texto = "N°", ancho = 38.dp, textAlign = TextAlign.Center)
                        CeldaEncabezadoTabla(texto = "Integrante", ancho = 175.dp)
                        actividades.forEach { act ->
                            val escalaStr = if (act.puntajeMaximo >= 100.0) "100 pts" else "5.0 pts"
                            CeldaEncabezadoTabla(
                                texto = "${act.titulo}\n($escalaStr)",
                                ancho = 128.dp,
                                textAlign = TextAlign.Center,
                            )
                        }
                        CeldaEncabezadoTabla(
                            texto = "Total\n(0.0 - 5.0)",
                            ancho = 102.dp,
                            textAlign = TextAlign.Center,
                        )
                        CeldaEncabezadoTabla(
                            texto = "Total\n(0 - 100)",
                            ancho = 102.dp,
                            textAlign = TextAlign.Center,
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))

                    // Filas de cada aprendiz
                    calificaciones.forEachIndexed { idx, res ->
                        val esUsuarioActual = res.aprendiz?.id != null && res.aprendiz.id == state.usuario?.id
                        val fondoFila = when {
                            esUsuarioActual -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
                            idx % 2 == 1 -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            else -> Color.Transparent
                        }
                        val mapaDetalle = res.detalle.associateBy { it.actividadId }

                        Row(
                            modifier = Modifier
                                .background(fondoFila)
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "${idx + 1}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(38.dp),
                            )

                            Column(
                                modifier = Modifier
                                    .width(175.dp)
                                    .padding(horizontal = 6.dp),
                            ) {
                                Text(
                                    text = res.aprendiz?.nombre ?: "Aprendiz ${idx + 1}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (esUsuarioActual) FontWeight.Bold else FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                res.aprendiz?.email?.takeIf { it.isNotBlank() }?.let { correo ->
                                    Text(
                                        text = correo,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }

                            actividades.forEach { act ->
                                val det = mapaDetalle[act.id]
                                Box(
                                    modifier = Modifier
                                        .width(128.dp)
                                        .padding(horizontal = 4.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    when {
                                        det?.exenta == true -> {
                                            Surface(
                                                color = c.especialContenedor,
                                                shape = RoundedCornerShape(8.dp),
                                            ) {
                                                Text(
                                                    text = "Exenta",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = c.especial,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                )
                                            }
                                        }
                                        det?.nota != null -> {
                                            val maxFmt = if (act.puntajeMaximo >= 100.0) "100" else "5.0"
                                            val aprobado = (det.nota / act.puntajeMaximo) >= 0.6
                                            val colorNota = if (aprobado) c.exito else MaterialTheme.colorScheme.error
                                            Text(
                                                text = "${UiFormatters.formatearNota(det.nota)} / $maxFmt",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = colorNota,
                                                textAlign = TextAlign.Center,
                                            )
                                        }
                                        else -> {
                                            Text(
                                                text = "0.00 (Pend.)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = c.advertencia,
                                                textAlign = TextAlign.Center,
                                            )
                                        }
                                    }
                                }
                            }

                            val aprobadoFinal = res.nota05 >= 3.0
                            val colorTotal = if (aprobadoFinal) c.exito else MaterialTheme.colorScheme.error

                            Box(
                                modifier = Modifier
                                    .width(102.dp)
                                    .padding(horizontal = 4.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Surface(
                                    color = colorTotal.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, colorTotal.copy(alpha = 0.4f)),
                                ) {
                                    Text(
                                        text = "${UiFormatters.formatearNota(res.nota05)} / 5.0",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = colorTotal,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .width(102.dp)
                                    .padding(horizontal = 4.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Surface(
                                    color = colorTotal.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, colorTotal.copy(alpha = 0.4f)),
                                ) {
                                    Text(
                                        text = "${UiFormatters.formatearNota(res.nota0100)} / 100",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = colorTotal,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }

                    // Fila resumen de promedio grupal
                    val prom05 = calificaciones.map { it.nota05 }.average()
                    val prom100 = calificaciones.map { it.nota0100 }.average()
                    Row(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f))
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Spacer(modifier = Modifier.width(38.dp))
                        Text(
                            text = "Promedio del grupo",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier
                                .width(175.dp)
                                .padding(horizontal = 6.dp),
                        )
                        repeat(actividades.size) {
                            Spacer(modifier = Modifier.width(128.dp))
                        }
                        Text(
                            text = "${UiFormatters.formatearNota(prom05)} / 5.0",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.width(102.dp),
                        )
                        Text(
                            text = "${UiFormatters.formatearNota(prom100)} / 100",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.width(102.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CeldaEncabezadoTabla(
    texto: String,
    ancho: androidx.compose.ui.unit.Dp,
    textAlign: TextAlign = TextAlign.Start,
) {
    Text(
        text = texto,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        textAlign = textAlign,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .width(ancho)
            .padding(horizontal = 6.dp),
    )
}

@Composable
private fun colorEstadoActividad(estado: EstadoEntrega?, esInstructor: Boolean): Color {
    val c = MaterialTheme.coloresExtendidos
    if (esInstructor) return MaterialTheme.colorScheme.primary
    return when (estado) {
        EstadoEntrega.PENDIENTE, null -> c.advertencia
        EstadoEntrega.ENTREGADA -> c.informacion
        EstadoEntrega.TARDIA -> MaterialTheme.colorScheme.error
        EstadoEntrega.CALIFICADA -> c.exito
        EstadoEntrega.EXENTA -> c.especial
    }
}
