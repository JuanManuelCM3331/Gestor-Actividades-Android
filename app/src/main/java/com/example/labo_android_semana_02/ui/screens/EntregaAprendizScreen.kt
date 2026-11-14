package com.example.labo_android_semana_02.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.labo_android_semana_02.domain.model.ArchivoAdjunto
import com.example.labo_android_semana_02.domain.model.EstadoEntrega
import com.example.labo_android_semana_02.ui.components.AppAssistChip
import com.example.labo_android_semana_02.ui.components.AppButton
import com.example.labo_android_semana_02.ui.components.AppIconButton
import com.example.labo_android_semana_02.ui.components.AppOutlinedButton
import com.example.labo_android_semana_02.ui.components.AppTextButton
import com.example.labo_android_semana_02.ui.components.BannerMensaje
import com.example.labo_android_semana_02.ui.components.EstadoCargandoPantalla
import com.example.labo_android_semana_02.ui.components.EstadoEntregaChip
import com.example.labo_android_semana_02.ui.components.IconoTintado
import com.example.labo_android_semana_02.ui.components.TituloConIcono
import com.example.labo_android_semana_02.ui.components.UiFormatters
import com.example.labo_android_semana_02.ui.components.WrapRow
import com.example.labo_android_semana_02.ui.components.colorParaTipoArchivo
import com.example.labo_android_semana_02.ui.theme.coloresExtendidos
import com.example.labo_android_semana_02.ui.viewmodel.EntregaAprendizUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntregaAprendizScreen(
    state: EntregaAprendizUiState,
    onVolver: () -> Unit,
    onDescripcionChange: (String) -> Unit,
    onConservarArchivosChange: (Boolean) -> Unit,
    onAgregarUri: (android.net.Uri) -> Unit,
    onQuitarArchivoNuevo: (Int) -> Unit,
    onEnviarEntrega: () -> Unit,
    onAbrirArchivo: (ArchivoAdjunto) -> Unit,
    convertirNotaA05Y0100: (Double, Double) -> Pair<Double, Double>,
    onLimpiarFeedback: () -> Unit,
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        uris.forEach(onAgregarUri)
    }

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
                    Text(
                        text = state.actividad?.titulo ?: "Detalle de actividad",
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
            )
        },
    ) { innerPadding ->
        val act = state.actividad
        if (act == null) {
            Box(modifier = Modifier.padding(innerPadding)) {
                EstadoCargandoPantalla("Cargando actividad...")
            }
            return@Scaffold
        }

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
                if (state.cargando || state.enviando) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primaryContainer,
                    )
                }

                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item {
                        if (state.error != null) {
                            BannerMensaje(state.error, true, onLimpiarFeedback, Modifier.padding(bottom = 8.dp))
                        } else if (state.mensaje != null) {
                            BannerMensaje(state.mensaje, false, onLimpiarFeedback, Modifier.padding(bottom = 8.dp))
                        }

                        Card(
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    IconoTintado(
                                        icono = Icons.AutoMirrored.Filled.Assignment,
                                        color = MaterialTheme.colorScheme.primary,
                                        tamano = 44.dp,
                                    )
                                    Text(
                                        act.titulo,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f),
                                    )
                                    act.estadoAprendiz?.let { EstadoEntregaChip(estado = it) }
                                }
                                if (act.descripcion.isNotBlank()) {
                                    Text(act.descripcion, style = MaterialTheme.typography.bodyMedium)
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
                                    )
                                }
                                if (act.adjuntos.isNotEmpty()) {
                                    Text("Archivos del instructor:", style = MaterialTheme.typography.labelMedium)
                                    WrapRow(horizontalSpacing = 6.dp, verticalSpacing = 6.dp) {
                                        act.adjuntos.forEach { adj ->
                                            AppAssistChip(
                                                onClick = { onAbrirArchivo(adj) },
                                                icon = Icons.Default.AttachFile,
                                                label = { Text(adj.nombreOriginal) },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    val entrega = act.miEntrega
                    if (entrega != null && (entrega.estado == EstadoEntrega.CALIFICADA || entrega.estado == EstadoEntrega.EXENTA)) {
                        item {
                            val exenta = entrega.estado == EstadoEntrega.EXENTA
                            val acento = if (exenta) MaterialTheme.coloresExtendidos.especial
                            else MaterialTheme.coloresExtendidos.exito
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (exenta) MaterialTheme.coloresExtendidos.especialContenedor
                                    else MaterialTheme.coloresExtendidos.exitoContenedor,
                                ),
                                border = BorderStroke(1.dp, acento.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (exenta) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.RemoveCircleOutline,
                                                contentDescription = null,
                                                tint = acento,
                                                modifier = Modifier.size(22.dp),
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                "Actividad exenta (entregada fuera del curso)",
                                                fontWeight = FontWeight.Bold,
                                                color = acento,
                                            )
                                        }
                                    } else if (entrega.nota != null) {
                                        val (n05, n0100) = convertirNotaA05Y0100(entrega.nota, act.puntajeMaximo)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconoTintado(
                                                icono = Icons.Default.Grade,
                                                color = acento,
                                                tamano = 40.dp,
                                                circular = true,
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = "Tu calificación: ${UiFormatters.formatearNota(entrega.nota)} / ${UiFormatters.formatearNota(act.puntajeMaximo)}",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = acento,
                                                )
                                                Text(
                                                    text = "Equivalencia: ${UiFormatters.formatearNota(n05)} / 5.00  (${UiFormatters.formatearNota(n0100)} / 100)",
                                                    style = MaterialTheme.typography.labelLarge,
                                                )
                                            }
                                        }
                                    }
                                    if (!entrega.retroalimentacion.isNullOrBlank()) {
                                        HorizontalDivider(color = acento.copy(alpha = 0.3f))
                                        Row(verticalAlignment = Alignment.Top) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Comment,
                                                contentDescription = null,
                                                tint = acento,
                                                modifier = Modifier.size(18.dp),
                                            )
                                            Text(
                                                " Retroalimentación: ${entrega.retroalimentacion}",
                                                style = MaterialTheme.typography.bodyMedium,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (entrega != null && entrega.archivos.isNotEmpty()) {
                        item {
                            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TituloConIcono(
                                        texto = "Evidencias enviadas anteriormente",
                                        icono = Icons.Default.Description,
                                        color = MaterialTheme.coloresExtendidos.informacion,
                                    )
                                    WrapRow(horizontalSpacing = 6.dp, verticalSpacing = 6.dp) {
                                        entrega.archivos.forEach { arch ->
                                            AppAssistChip(
                                                onClick = { onAbrirArchivo(arch) },
                                                icon = Icons.Default.AttachFile,
                                                label = { Text("${arch.nombreOriginal} (${UiFormatters.formatearTamano(arch.tamanoBytes)})") },
                                            )
                                        }
                                    }
                                    if (!state.bloqueadaPorCalificacion) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(
                                                checked = state.conservarArchivosPrevios,
                                                onCheckedChange = onConservarArchivosChange,
                                                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                                            )
                                            Text(
                                                text = "Conservar evidencias anteriores al reenviar",
                                                style = MaterialTheme.typography.bodySmall,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                TituloConIcono(
                                    texto = if (state.bloqueadaPorCalificacion) {
                                        "Entrega cerrada por calificación"
                                    } else if (entrega != null) {
                                        "Editar o reenviar evidencias"
                                    } else {
                                        "Subir entrega"
                                    },
                                    icono = if (state.bloqueadaPorCalificacion) Icons.Default.Lock else Icons.Default.Upload,
                                    color = if (state.bloqueadaPorCalificacion) MaterialTheme.coloresExtendidos.neutro
                                    else MaterialTheme.colorScheme.primary,
                                )

                                OutlinedTextField(
                                    value = state.descripcion,
                                    onValueChange = onDescripcionChange,
                                    enabled = !state.bloqueadaPorCalificacion,
                                    label = { Text("Comentarios / descripción de la entrega") },
                                    minLines = 3,
                                    modifier = Modifier.fillMaxWidth(),
                                )

                                if (!state.bloqueadaPorCalificacion) {
                                    AppOutlinedButton(
                                        onClick = { launcher.launch(arrayOf("*/*")) },
                                        icon = Icons.Default.AttachFile,
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text("Adjuntar archivos (PDF, Office, imagen, ZIP, video • máx. 20 MB c/u)")
                                    }

                                    state.archivosNuevos.forEachIndexed { idx, archivo ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Description,
                                                contentDescription = null,
                                                tint = colorParaTipoArchivo(archivo.nombre),
                                                modifier = Modifier.size(18.dp),
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "${archivo.nombre} (${UiFormatters.formatearTamano(archivo.tamanoBytes)})",
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.weight(1f),
                                            )
                                            AppTextButton(onClick = { onQuitarArchivoNuevo(idx) }) {
                                                Text("Quitar", color = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }

                                    AppButton(
                                        onClick = onEnviarEntrega,
                                        enabled = !state.enviando,
                                        icon = Icons.AutoMirrored.Filled.Send,
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text(if (entrega == null) "Enviar entrega" else "Guardar y reenviar")
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }
}
