package com.example.labo_android_semana_02.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.labo_android_semana_02.domain.model.ArchivoAdjunto
import com.example.labo_android_semana_02.domain.model.EntregaInstructorItem
import com.example.labo_android_semana_02.ui.components.BannerMensaje
import com.example.labo_android_semana_02.ui.components.EstadoEntregaChip
import com.example.labo_android_semana_02.ui.components.EstadoVacioTarjeta
import com.example.labo_android_semana_02.ui.components.UiFormatters
import com.example.labo_android_semana_02.ui.components.WrapRow
import com.example.labo_android_semana_02.ui.viewmodel.ActividadEntregasUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActividadEntregasScreen(
    state: ActividadEntregasUiState,
    onVolver: () -> Unit,
    onRefrescar: () -> Unit,
    onAbrirArchivo: (ArchivoAdjunto) -> Unit,
    onCalificar: (Int, String, String, Boolean) -> Unit,
    onLimpiarFeedback: () -> Unit,
) {
    var entregaACalificar by remember { mutableStateOf<EntregaInstructorItem?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                title = { Text(state.actividad?.titulo ?: "Entregas de aprendices") },
                actions = {
                    IconButton(onClick = onRefrescar) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualizar")
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
                if (state.cargando) {
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

                        state.actividad?.let { act ->
                            Text(
                                text = "Puntaje máximo: ${UiFormatters.formatearNota(act.puntajeMaximo)} • Límite: ${UiFormatters.formatearFechaHora(act.fechaLimite)}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }

                    if (state.entregas.isEmpty() && !state.cargando) {
                        item {
                            EstadoVacioTarjeta(
                                titulo = "No hay aprendices inscritos",
                                descripcion = "Cuando los aprendices se unan al curso verás aquí sus entregas y estado.",
                            )
                        }
                    }

                    items(state.entregas, key = { it.aprendizId }) { item ->
                        Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.aprendizNombre, fontWeight = FontWeight.Bold)
                                        Text(
                                            item.aprendizEmail,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    EstadoEntregaChip(estado = item.estado)
                                }

                                if (item.descripcion.isNotBlank()) {
                                    Text(item.descripcion, style = MaterialTheme.typography.bodySmall)
                                } else if (item.entregaId == null) {
                                    Text(
                                        "Sin entrega enviada aún.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }

                                if (item.archivos.isNotEmpty()) {
                                    WrapRow(
                                        horizontalSpacing = 6.dp,
                                        verticalSpacing = 6.dp,
                                    ) {
                                        item.archivos.forEach { arch ->
                                            AssistChip(
                                                onClick = { onAbrirArchivo(arch) },
                                                label = {
                                                    Text("${arch.nombreOriginal} (${UiFormatters.formatearTamano(arch.tamanoBytes)})")
                                                },
                                            )
                                        }
                                    }
                                }

                                if (item.nota != null) {
                                    Text(
                                        text = "Nota asignada: ${UiFormatters.formatearNota(item.nota)} / ${UiFormatters.formatearNota(state.actividad?.puntajeMaximo)}",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                } else if (item.exenta) {
                                    Text(
                                        text = "Exenta (entregada fuera del curso; excluida del promedio)",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.secondary,
                                    )
                                }

                                if (!item.retroalimentacion.isNullOrBlank()) {
                                    Text(
                                        text = "Retroalimentación: ${item.retroalimentacion}",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }

                                OutlinedButton(
                                    onClick = { entregaACalificar = item },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(if (item.nota != null || item.exenta) "Editar calificación" else "Calificar / Marcar exenta")
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }

    entregaACalificar?.let { item ->
        DialogoCalificarEntrega(
            item = item,
            puntajeMaximo = state.actividad?.puntajeMaximo ?: 5.0,
            onDismiss = { entregaACalificar = null },
            onGuardar = { notaTxt, retro, exenta ->
                onCalificar(item.aprendizId, notaTxt, retro, exenta)
                entregaACalificar = null
            },
        )
    }
}

@Composable
private fun DialogoCalificarEntrega(
    item: EntregaInstructorItem,
    puntajeMaximo: Double,
    onDismiss: () -> Unit,
    onGuardar: (String, String, Boolean) -> Unit,
) {
    var notaTexto by remember { mutableStateOf(item.nota?.toString().orEmpty()) }
    var retroalimentacion by remember { mutableStateOf(item.retroalimentacion.orEmpty()) }
    var exenta by remember { mutableStateOf(item.exenta) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Calificar a ${item.aprendizNombre}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = exenta, onCheckedChange = { exenta = it })
                    Text(
                        text = "Exenta (entregada fuera del curso)",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (!exenta) {
                    OutlinedTextField(
                        value = notaTexto,
                        onValueChange = { notaTexto = it },
                        label = { Text("Nota (0 a ${UiFormatters.formatearNota(puntajeMaximo)})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                OutlinedTextField(
                    value = retroalimentacion,
                    onValueChange = { retroalimentacion = it },
                    label = { Text("Retroalimentación para el aprendiz") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(onClick = { onGuardar(notaTexto, retroalimentacion, exenta) }) {
                Text("Guardar calificación")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}
