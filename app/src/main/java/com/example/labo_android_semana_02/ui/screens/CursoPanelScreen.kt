package com.example.labo_android_semana_02.ui.screens

import com.example.labo_android_semana_02.ui.components.AppButton
import com.example.labo_android_semana_02.ui.components.AppFilterChip
import com.example.labo_android_semana_02.ui.components.AppIconButton
import com.example.labo_android_semana_02.ui.components.AppOutlinedButton
import com.example.labo_android_semana_02.ui.components.AppTextButton

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.foundation.background
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.TopAppBarDefaults
import com.example.labo_android_semana_02.ui.components.IconoTintado
import com.example.labo_android_semana_02.ui.components.colorParaTipoArchivo
import com.example.labo_android_semana_02.ui.theme.coloresExtendidos
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.labo_android_semana_02.domain.model.Actividad
import com.example.labo_android_semana_02.domain.model.ArchivoAdjunto
import com.example.labo_android_semana_02.domain.model.ArchivoLocal
import com.example.labo_android_semana_02.domain.model.MaterialApoyo
import com.example.labo_android_semana_02.domain.model.RolUsuario
import com.example.labo_android_semana_02.ui.components.BannerMensaje
import com.example.labo_android_semana_02.ui.components.UiFormatters
import com.example.labo_android_semana_02.ui.viewmodel.CursoPanelUiState
import java.io.File
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CursoPanelScreen(
    state: CursoPanelUiState,
    onVolver: () -> Unit,
    onRefrescar: () -> Unit,
    onRegenerarCodigo: () -> Unit,
    onCambiarEstadoCodigo: (Boolean) -> Unit,
    onValidarYLeerArchivo: (Uri) -> ArchivoLocal?,
    onGuardarActividad: (Int?, String, String, Long, String, List<ArchivoLocal>) -> Unit,
    onEliminarActividad: (Int) -> Unit,
    onAbrirEntregasInstructor: (Int) -> Unit,
    onAbrirEntregaAprendiz: (Int) -> Unit,
    onAbrirAdjunto: (ArchivoAdjunto) -> Unit,
    onCrearMaterial: (String, String, Boolean, String, ArchivoLocal?) -> Unit,
    onEliminarMaterial: (Int) -> Unit,
    onAbrirMaterial: (MaterialApoyo) -> Unit,
    onCambiarModoForo: (Boolean) -> Unit,
    onCambiarFiltroAprendizForo: (Int?) -> Unit,
    onChatTextoChange: (String) -> Unit,
    onChatEnlaceChange: (String) -> Unit,
    onAlternarCampoEnlaceChat: () -> Unit,
    onAgregarArchivoChatUri: (Uri) -> Unit,
    onAgregarAudioGrabadoChat: (File) -> Unit,
    onQuitarArchivoChat: (Int) -> Unit,
    onEnviarMensajeChat: () -> Unit,
    onAbrirEnlaceExterno: (String) -> Unit,
    onDescargarArchivoEnCache: suspend (ArchivoAdjunto) -> Result<File>,
    onMostrarError: (String) -> Unit,
    onExpulsarEstudiante: (Int) -> Unit,
    onSalirseDelCurso: () -> Unit,
    onConvertir05A100: (Double) -> Double,
    onLimpiarFeedback: () -> Unit,
) {
    val esInstructor = state.usuario?.rol == RolUsuario.INSTRUCTOR
    var pestanaActiva by remember { mutableIntStateOf(0) }
    var dialogoActividad by remember { mutableStateOf<Pair<Boolean, Actividad?>?>(null) }
    var dialogoMaterial by remember { mutableStateOf(false) }

    val pestanas = listOf("Actividades", "Material", "Foro", "Personas")
    val iconosPestanas = listOf(
        Icons.AutoMirrored.Filled.Assignment,
        Icons.Default.FolderOpen,
        Icons.Default.Forum,
        Icons.Default.Groups,
    )
    val coloresPestanas = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.coloresExtendidos.advertencia,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.coloresExtendidos.especial,
    )

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                ),
                navigationIcon = {
                    AppIconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                title = {
                    Column {
                        Text(
                            text = state.curso?.nombre ?: "Panel del curso",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        state.curso?.let {
                            Text(
                                text = "Prof. ${it.instructorNombre}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                },
                actions = {
                    AppIconButton(onClick = onRefrescar) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Actualizar",
                            tint = MaterialTheme.coloresExtendidos.informacion,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 840.dp)
                    .fillMaxSize(),
            ) {
                if (state.cargando || state.procesandoArchivo) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primaryContainer,
                    )
                }

                TabRow(
                    selectedTabIndex = pestanaActiva,
                    containerColor = MaterialTheme.colorScheme.background,
                    indicator = { posiciones ->
                        Box(
                            modifier = Modifier
                                .tabIndicatorOffset(posiciones[pestanaActiva])
                                .height(3.dp)
                                .background(
                                    color = coloresPestanas[pestanaActiva],
                                    shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp),
                                ),
                        )
                    },
                ) {
                    pestanas.forEachIndexed { idx, label ->
                        Tab(
                            selected = pestanaActiva == idx,
                            onClick = { pestanaActiva = idx },
                            selectedContentColor = coloresPestanas[idx],
                            unselectedContentColor = coloresPestanas[idx].copy(alpha = 0.6f),
                            icon = {
                                Icon(
                                    imageVector = iconosPestanas[idx],
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp),
                                )
                            },
                            text = {
                                Text(
                                    text = label,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            },
                        )
                    }
                }

                if (pestanaActiva == 2) {
                    if (state.error != null) {
                        BannerMensaje(
                            texto = state.error,
                            esError = true,
                            onDismiss = onLimpiarFeedback,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        )
                    } else if (state.mensaje != null) {
                        BannerMensaje(
                            texto = state.mensaje,
                            esError = false,
                            onDismiss = onLimpiarFeedback,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        )
                    }

                    TabForoContenido(
                        state = state,
                        esInstructor = esInstructor,
                        onCambiarModoForo = onCambiarModoForo,
                        onCambiarFiltroAprendiz = onCambiarFiltroAprendizForo,
                        onChatTextoChange = onChatTextoChange,
                        onChatEnlaceChange = onChatEnlaceChange,
                        onAlternarCampoEnlaceChat = onAlternarCampoEnlaceChat,
                        onAgregarArchivoChatUri = onAgregarArchivoChatUri,
                        onAgregarAudioGrabadoChat = onAgregarAudioGrabadoChat,
                        onQuitarArchivoChat = onQuitarArchivoChat,
                        onEnviarMensajeChat = onEnviarMensajeChat,
                        onAbrirAdjunto = onAbrirAdjunto,
                        onAbrirEnlace = onAbrirEnlaceExterno,
                        onDescargarEnCache = onDescargarArchivoEnCache,
                        onMostrarError = onMostrarError,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    ) {
                        item {
                            if (state.error != null) {
                                BannerMensaje(
                                    texto = state.error,
                                    esError = true,
                                    onDismiss = onLimpiarFeedback,
                                    modifier = Modifier.padding(bottom = 8.dp),
                                )
                            } else if (state.mensaje != null) {
                                BannerMensaje(
                                    texto = state.mensaje,
                                    esError = false,
                                    onDismiss = onLimpiarFeedback,
                                    modifier = Modifier.padding(bottom = 8.dp),
                                )
                            }

                            if (esInstructor && state.curso != null) {
                                TarjetaCodigoAccesoInstructor(
                                    codigo = state.curso.codigoAcceso ?: "—",
                                    activo = state.curso.codigoActivo,
                                    onToggleActivo = onCambiarEstadoCodigo,
                                    onRegenerar = onRegenerarCodigo,
                                )
                            } else if (!esInstructor && state.miCalificacion != null) {
                                TarjetaResumenNotasAprendiz(state = state)
                            }
                        }

                        item {
                            when (pestanaActiva) {
                                0 -> TabActividadesContenido(
                                    state = state,
                                    esInstructor = esInstructor,
                                    onNuevaActividad = { dialogoActividad = true to null },
                                    onEditarActividad = { act -> dialogoActividad = true to act },
                                    onEliminarActividad = onEliminarActividad,
                                    onAbrirEntregasInstructor = onAbrirEntregasInstructor,
                                    onAbrirEntregaAprendiz = onAbrirEntregaAprendiz,
                                    onAbrirAdjunto = onAbrirAdjunto,
                                )
                                1 -> TabMaterialContenido(
                                    state = state,
                                    esInstructor = esInstructor,
                                    onNuevoMaterial = { dialogoMaterial = true },
                                    onEliminarMaterial = onEliminarMaterial,
                                    onAbrirMaterial = onAbrirMaterial,
                                )
                                else -> TabPersonasContenido(
                                    state = state,
                                    esInstructor = esInstructor,
                                    convertir05A100 = onConvertir05A100,
                                    onExpulsarEstudiante = onExpulsarEstudiante,
                                    onSalirseDelCurso = onSalirseDelCurso,
                                )
                            }
                        }

                        item { Spacer(modifier = Modifier.height(24.dp)) }
                    }
                }
            }
        }
    }

    dialogoActividad?.let { (_, editar) ->
        DialogoCrearEditarActividad(
            actividad = editar,
            onValidarYLeerArchivo = onValidarYLeerArchivo,
            onDismiss = { dialogoActividad = null },
            onGuardar = { titulo, desc, fechaMs, puntajeTxt, adjuntos ->
                onGuardarActividad(editar?.id, titulo, desc, fechaMs, puntajeTxt, adjuntos)
                dialogoActividad = null
            },
        )
    }

    if (dialogoMaterial) {
        DialogoCrearMaterial(
            onValidarYLeerArchivo = onValidarYLeerArchivo,
            onDismiss = { dialogoMaterial = false },
            onGuardar = { titulo, desc, esEnlace, url, archivo ->
                onCrearMaterial(titulo, desc, esEnlace, url, archivo)
                dialogoMaterial = false
            },
        )
    }
}

@Composable
private fun TarjetaCodigoAccesoInstructor(
    codigo: String,
    activo: Boolean,
    onToggleActivo: (Boolean) -> Unit,
    onRegenerar: () -> Unit,
) {
    val acento = if (activo) MaterialTheme.coloresExtendidos.exito else MaterialTheme.coloresExtendidos.neutro
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconoTintado(icono = Icons.Default.Key, color = acento, tamano = 44.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "Código de acceso al curso",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        Text(
                            codigo,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (activo) "Activo" else "Inactivo",
                        style = MaterialTheme.typography.labelMedium,
                        color = acento,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Switch(
                        checked = activo,
                        onCheckedChange = onToggleActivo,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
            AppOutlinedButton(
                onClick = onRegenerar,
                icon = Icons.Default.Refresh,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Regenerar código de acceso")
            }
        }
    }
}

@Composable
private fun TarjetaResumenNotasAprendiz(state: CursoPanelUiState) {
    val res = state.miCalificacion ?: return
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconoTintado(
                icono = Icons.Default.Grade,
                color = MaterialTheme.coloresExtendidos.advertencia,
                tamano = 48.dp,
                circular = true,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Tu promedio actual del curso",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = "${UiFormatters.formatearNota(res.nota05)} / 5.00   •   ${UiFormatters.formatearNota(res.nota0100)} / 100",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = "Evaluables: ${res.actividadesEvaluables} • Calificadas: ${res.actividadesCalificadas} • Exentas excluidas: ${res.actividadesExentas}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogoCrearEditarActividad(
    actividad: Actividad?,
    onValidarYLeerArchivo: (Uri) -> ArchivoLocal?,
    onDismiss: () -> Unit,
    onGuardar: (String, String, Long, String, List<ArchivoLocal>) -> Unit,
) {
    var titulo by remember { mutableStateOf(actividad?.titulo.orEmpty()) }
    var descripcion by remember { mutableStateOf(actividad?.descripcion.orEmpty()) }
    var puntajeTxt by remember { mutableStateOf(actividad?.puntajeMaximo?.toString() ?: "5.0") }
    var fechaLimiteMs by remember {
        mutableLongStateOf(actividad?.fechaLimite ?: (System.currentTimeMillis() + 5L * 86_400_000L))
    }
    var archivos by remember { mutableStateOf<List<ArchivoLocal>>(emptyList()) }
    var mostrarDatePicker by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        val validados = uris.mapNotNull { onValidarYLeerArchivo(it) }
        archivos = (archivos + validados).take(10)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = if (actividad == null) Icons.Default.AddCircle else Icons.Default.Edit,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = { Text(if (actividad == null) "Nueva actividad" else "Editar actividad") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = { Text("Título de la actividad") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Instrucciones / descripción") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = puntajeTxt,
                    onValueChange = { puntajeTxt = it },
                    label = { Text("Puntaje máximo (ej. 5.0 o 100)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                AppOutlinedButton(
                    onClick = { mostrarDatePicker = true },
                    icon = Icons.Default.CalendarMonth,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Fecha límite: ${UiFormatters.formatearFechaCorta(fechaLimiteMs)} (23:59)")
                }
                AppOutlinedButton(
                    onClick = { launcher.launch(arrayOf("*/*")) },
                    icon = Icons.Default.AttachFile,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Adjuntar archivos (máx. 20 MB c/u)")
                }
                archivos.forEachIndexed { idx, file ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = colorParaTipoArchivo(file.nombre),
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${file.nombre} (${UiFormatters.formatearTamano(file.tamanoBytes)})",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.weight(1f),
                        )
                        AppTextButton(onClick = { archivos = archivos.filterIndexed { i, _ -> i != idx } }) {
                            Text("Quitar", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        },
        confirmButton = {
            AppButton(
                onClick = { onGuardar(titulo, descripcion, fechaLimiteMs, puntajeTxt, archivos) },
                icon = Icons.Default.CheckCircle,
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            AppTextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )

    if (mostrarDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = fechaLimiteMs)
        DatePickerDialog(
            onDismissRequest = { mostrarDatePicker = false },
            confirmButton = {
                AppTextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { utcMillis ->
                            val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = utcMillis
                            }
                            val localCal = Calendar.getInstance().apply {
                                set(
                                    utcCal.get(Calendar.YEAR),
                                    utcCal.get(Calendar.MONTH),
                                    utcCal.get(Calendar.DAY_OF_MONTH),
                                    23,
                                    59,
                                    0,
                                )
                            }
                            fechaLimiteMs = localCal.timeInMillis
                        }
                        mostrarDatePicker = false
                    },
                ) { Text("Aceptar") }
            },
            dismissButton = {
                AppTextButton(onClick = { mostrarDatePicker = false }) { Text("Cancelar") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun DialogoCrearMaterial(
    onValidarYLeerArchivo: (Uri) -> ArchivoLocal?,
    onDismiss: () -> Unit,
    onGuardar: (String, String, Boolean, String, ArchivoLocal?) -> Unit,
) {
    var titulo by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var esEnlace by remember { mutableStateOf(false) }
    var enlaceUrl by remember { mutableStateOf("https://") }
    var archivo by remember { mutableStateOf<ArchivoLocal?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) archivo = onValidarYLeerArchivo(uri)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.FolderOpen, contentDescription = null, tint = MaterialTheme.coloresExtendidos.advertencia)
        },
        title = { Text("Subir material de apoyo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppFilterChip(
                        selected = !esEnlace,
                        onClick = { esEnlace = false },
                        icon = Icons.Default.AttachFile,
                        label = { Text("Archivo") },
                    )
                    AppFilterChip(
                        selected = esEnlace,
                        onClick = { esEnlace = true },
                        icon = Icons.Default.Link,
                        label = { Text("Enlace URL") },
                    )
                }
                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = { Text("Título del material") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (esEnlace) {
                    OutlinedTextField(
                        value = enlaceUrl,
                        onValueChange = { enlaceUrl = it },
                        label = { Text("URL (https://...)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    AppOutlinedButton(
                        onClick = { launcher.launch(arrayOf("*/*")) },
                        icon = Icons.Default.AttachFile,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(archivo?.let { "${it.nombre} (${UiFormatters.formatearTamano(it.tamanoBytes)})" }
                            ?: "Seleccionar archivo (máx. 20 MB)")
                    }
                }
            }
        },
        confirmButton = {
            AppButton(
                onClick = { onGuardar(titulo, descripcion, esEnlace, enlaceUrl, archivo) },
                icon = Icons.AutoMirrored.Filled.Send,
            ) {
                Text("Publicar")
            }
        },
        dismissButton = {
            AppTextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}

