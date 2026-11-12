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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.labo_android_semana_02.domain.model.RolUsuario
import com.example.labo_android_semana_02.domain.model.TipoHiloForo
import com.example.labo_android_semana_02.ui.components.BannerMensaje
import com.example.labo_android_semana_02.ui.components.UiFormatters
import com.example.labo_android_semana_02.ui.viewmodel.ForoHiloUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForoHiloScreen(
    state: ForoHiloUiState,
    onVolver: () -> Unit,
    onRefrescar: () -> Unit,
    onRespuestaChange: (String) -> Unit,
    onEnviarRespuesta: () -> Unit,
    onLimpiarError: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                title = { Text(state.hilo?.titulo ?: "Conversación del foro") },
                actions = {
                    IconButton(onClick = onRefrescar) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualizar")
                    }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = state.respuestaTexto,
                        onValueChange = onRespuestaChange,
                        placeholder = { Text("Escribe tu respuesta...") },
                        maxLines = 3,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = onEnviarRespuesta,
                        enabled = !state.enviando && state.respuestaTexto.isNotBlank(),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar")
                    }
                }
            }
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
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item {
                        if (state.error != null) {
                            BannerMensaje(state.error, true, onLimpiarError, Modifier.padding(bottom = 8.dp))
                        }

                        state.hilo?.let { hilo ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Text(
                                        text = if (hilo.tipo == TipoHiloForo.PRIVADO) "Hilo privado 1 a 1" else "Hilo público",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Text(hilo.titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(hilo.contenido, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = "${hilo.autorNombre} (${if (hilo.autorRol == RolUsuario.INSTRUCTOR) "Instructor" else "Aprendiz"}) • ${UiFormatters.formatearFechaHora(hilo.createdAt)}",
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Respuestas (${state.mensajes.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }

                    items(state.mensajes, key = { it.id }) { msg ->
                        Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = "${msg.autorNombre} (${if (msg.autorRol == RolUsuario.INSTRUCTOR) "Instructor" else "Aprendiz"})",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Text(
                                        text = UiFormatters.formatearFechaHora(msg.createdAt),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Text(msg.contenido, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}
