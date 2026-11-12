package com.example.labo_android_semana_02.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.labo_android_semana_02.domain.model.ArchivoAdjunto
import com.example.labo_android_semana_02.domain.model.EstadoEntrega
import com.example.labo_android_semana_02.ui.components.BannerMensaje
import com.example.labo_android_semana_02.ui.components.EstadoCargandoPantalla
import com.example.labo_android_semana_02.ui.components.EstadoEntregaChip
import com.example.labo_android_semana_02.ui.components.UiFormatters
import com.example.labo_android_semana_02.ui.components.WrapRow
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
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                title = { Text(state.actividad?.titulo ?: "Detalle de actividad") },
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
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
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

                        Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(act.titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    act.estadoAprendiz?.let { EstadoEntregaChip(estado = it) }
                                }
                                if (act.descripcion.isNotBlank()) {
                                    Text(act.descripcion, style = MaterialTheme.typography.bodyMedium)
                                }
                                Text(
                                    text = "Límite: ${UiFormatters.formatearFechaHora(act.fechaLimite)} • Puntaje máximo: ${UiFormatters.formatearNota(act.puntajeMaximo)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                if (act.adjuntos.isNotEmpty()) {
                                    Text("Archivos del instructor:", style = MaterialTheme.typography.labelMedium)
                                    WrapRow(horizontalSpacing = 6.dp, verticalSpacing = 6.dp) {
                                        act.adjuntos.forEach { adj ->
                                            AssistChip(
                                                onClick = { onAbrirArchivo(adj) },
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
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (entrega.estado == EstadoEntrega.EXENTA) {
                                        Text("Actividad exenta (entregada fuera del curso)", fontWeight = FontWeight.Bold)
                                    } else if (entrega.nota != null) {
                                        val (n05, n0100) = convertirNotaA05Y0100(entrega.nota, act.puntajeMaximo)
                                        Text(
                                            text = "Tu calificación: ${UiFormatters.formatearNota(entrega.nota)} / ${UiFormatters.formatearNota(act.puntajeMaximo)}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Text(
                                            text = "Equivalencia: ${UiFormatters.formatearNota(n05)} / 5.00  (${UiFormatters.formatearNota(n0100)} / 100)",
                                            style = MaterialTheme.typography.labelLarge,
                                        )
                                    }
                                    if (!entrega.retroalimentacion.isNullOrBlank()) {
                                        HorizontalDivider()
                                        Text("Retroalimentación: ${entrega.retroalimentacion}", style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }

                    if (entrega != null && entrega.archivos.isNotEmpty()) {
                        item {
                            Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Evidencias enviadas anteriormente:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                                    WrapRow(horizontalSpacing = 6.dp, verticalSpacing = 6.dp) {
                                        entrega.archivos.forEach { arch ->
                                            AssistChip(
                                                onClick = { onAbrirArchivo(arch) },
                                                label = { Text("${arch.nombreOriginal} (${UiFormatters.formatearTamano(arch.tamanoBytes)})") },
                                            )
                                        }
                                    }
                                    if (!state.bloqueadaPorCalificacion) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(
                                                checked = state.conservarArchivosPrevios,
                                                onCheckedChange = onConservarArchivosChange,
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
                        Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = if (state.bloqueadaPorCalificacion) {
                                        "Entrega cerrada por calificación"
                                    } else if (entrega != null) {
                                        "Editar o reenviar evidencias"
                                    } else {
                                        "Subir entrega"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
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
                                    OutlinedButton(
                                        onClick = { launcher.launch(arrayOf("*/*")) },
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
                                            Text(
                                                text = "${archivo.nombre} (${UiFormatters.formatearTamano(archivo.tamanoBytes)})",
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.weight(1f),
                                            )
                                            TextButton(onClick = { onQuitarArchivoNuevo(idx) }) {
                                                Text("Quitar")
                                            }
                                        }
                                    }

                                    Button(
                                        onClick = onEnviarEntrega,
                                        enabled = !state.enviando,
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
