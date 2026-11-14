package com.example.labo_android_semana_02.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.labo_android_semana_02.domain.model.RolUsuario
import com.example.labo_android_semana_02.domain.model.TipoHiloForo
import com.example.labo_android_semana_02.ui.components.AppIconButton
import com.example.labo_android_semana_02.ui.components.BannerMensaje
import com.example.labo_android_semana_02.ui.components.IconoTintado
import com.example.labo_android_semana_02.ui.components.UiFormatters
import com.example.labo_android_semana_02.ui.theme.coloresExtendidos
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
    val puedeEnviar = !state.enviando && state.respuestaTexto.isNotBlank()

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
                        text = state.hilo?.titulo ?: "Conversación del foro",
                        style = MaterialTheme.typography.titleMedium,
                    )
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
        bottomBar = {
            Surface(
                color = MaterialTheme.coloresExtendidos.superficieTarjeta,
                tonalElevation = 3.dp,
                shadowElevation = 6.dp,
            ) {
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
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.weight(1f),
                    )
                    Surface(
                        color = if (puedeEnviar) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(50),
                    ) {
                        AppIconButton(
                            onClick = onEnviarRespuesta,
                            enabled = puedeEnviar,
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Enviar",
                                tint = if (puedeEnviar) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
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
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primaryContainer,
                    )
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
                            val privado = hilo.tipo == TipoHiloForo.PRIVADO
                            val acento = if (privado) MaterialTheme.colorScheme.tertiary
                            else MaterialTheme.colorScheme.primary
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (privado) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)
                                    else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                                ),
                                border = BorderStroke(1.dp, acento.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (privado) Icons.Default.Lock else Icons.Default.Public,
                                            contentDescription = null,
                                            tint = acento,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (privado) "Hilo privado 1 a 1" else "Hilo público",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = acento,
                                        )
                                    }
                                    Text(hilo.titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(hilo.contenido, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = "${hilo.autorNombre} (${if (hilo.autorRol == RolUsuario.INSTRUCTOR) "Instructor" else "Aprendiz"}) • ${UiFormatters.formatearFechaHora(hilo.createdAt)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        val esInstructor = msg.autorRol == RolUsuario.INSTRUCTOR
                        val acento = if (esInstructor) MaterialTheme.colorScheme.primary
                        else MaterialTheme.coloresExtendidos.especial
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, acento.copy(alpha = 0.28f)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                IconoTintado(
                                    icono = if (esInstructor) Icons.Default.School else Icons.Default.Person,
                                    color = acento,
                                    tamano = 36.dp,
                                    circular = true,
                                )
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(
                                            text = "${msg.autorNombre} (${if (esInstructor) "Instructor" else "Aprendiz"})",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = acento,
                                            modifier = Modifier.weight(1f),
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
}
