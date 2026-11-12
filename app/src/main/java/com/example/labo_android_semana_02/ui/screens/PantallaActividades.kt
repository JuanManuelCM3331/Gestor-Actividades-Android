package com.example.labo_android_semana_02.ui.screens

import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.labo_android_semana_02.domain.Categoria
import com.example.labo_android_semana_02.domain.Reporte
import com.example.labo_android_semana_02.ui.ListadoUiState
import com.example.labo_android_semana_02.ui.calcularResumen
import com.example.labo_android_semana_02.ui.components.TarjetaReporte
import com.example.labo_android_semana_02.ui.formatoPortapapeles

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaActividades(
    uiState: ListadoUiState,
    busqueda: String,
    onBusquedaChange: (String) -> Unit,
    onReporteClick: (Reporte) -> Unit,
    onDeleteReporte: (Reporte) -> Unit,
    onEditReporte: (Reporte) -> Unit,
    onToggleStatus: (Reporte) -> Unit,
    onAddClick: () -> Unit,
    onRetry: () -> Unit,
    temaOscuro: Boolean = false,
    onToggleTema: (Boolean) -> Unit = {},
    onToggleFavorito: (Reporte) -> Unit = {},
    categorias: List<Categoria> = emptyList(),
    categoriaSeleccionadaId: Int? = null,
    onCategoriaSeleccionada: (Int?) -> Unit = {},
    ordenActual: String = "FECHA_DESC",
    onToggleOrden: () -> Unit = {},
    syncEstado: String? = null,
    sincronizando: Boolean = false,
    onSyncClick: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current
    val triggerHaptic = {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
    }

    var reporteAEliminar by remember { mutableStateOf<Reporte?>(null) }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val onCopiar: (Reporte) -> Unit = { reporte ->
        clipboardManager.setText(AnnotatedString(formatoPortapapeles(reporte)))
        Toast.makeText(context, "Copiado al portapapeles", Toast.LENGTH_SHORT).show()
    }

    val reportes = (uiState as? ListadoUiState.Contenido)?.reportes ?: emptyList()

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("ReportaCTMA", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    actions = {
                        IconButton(onClick = {
                            triggerHaptic()
                            onToggleOrden()
                        }) {
                            Icon(
                                imageVector = Icons.Default.SwapVert,
                                contentDescription = if (ordenActual == "FECHA_ASC") "Orden ascendente" else "Orden descendente"
                            )
                        }
                        IconButton(
                            onClick = {
                                triggerHaptic()
                                onSyncClick()
                            },
                            enabled = !sincronizando
                        ) {
                            if (sincronizando) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Sincronizar con MySQL"
                                )
                            }
                        }
                        Icon(
                            imageVector = if (temaOscuro) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Switch(
                            checked = temaOscuro,
                            onCheckedChange = {
                                triggerHaptic()
                                onToggleTema(it)
                            },
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .semantics { contentDescription = "Cambiar tema de la aplicación" }
                        )
                    }
                )
                OutlinedTextField(
                    value = busqueda,
                    onValueChange = onBusquedaChange,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                    placeholder = { Text("Buscar reporte...") },
                    singleLine = true
                )
                if (categorias.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = categoriaSeleccionadaId == null,
                            onClick = {
                                triggerHaptic()
                                onCategoriaSeleccionada(null)
                            },
                            label = { Text("Todas") }
                        )
                        categorias.forEach { cat ->
                            FilterChip(
                                selected = categoriaSeleccionadaId == cat.id,
                                onClick = {
                                    triggerHaptic()
                                    onCategoriaSeleccionada(cat.id)
                                },
                                label = { Text("${cat.icono} ${cat.nombre}") }
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    triggerHaptic()
                    onAddClick()
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Text("+", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            syncEstado?.let { msg ->
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onSyncClick) {
                            Text("Reintentar", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            if (uiState !is ListadoUiState.Cargando) {
                val resumen = reportes.calcularResumen()
                ResumenContadores(
                    pendientes = resumen.pendientes,
                    completados = resumen.completados
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when (uiState) {
                    is ListadoUiState.Cargando -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Cargando reportes...", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    is ListadoUiState.Vacio -> {
                        EstadoInformativo(
                            mensaje = "No se encontraron reportes.",
                            accionTexto = "Crear mi primer reporte",
                            onAccion = onAddClick
                        )
                    }
                    is ListadoUiState.Error -> {
                        EstadoInformativo(
                            mensaje = uiState.mensaje,
                            accionTexto = "Reintentar",
                            onAccion = onRetry
                        )
                    }
                    is ListadoUiState.Contenido -> {
                        val pendientes = reportes.filter { !it.resuelto }
                        val completados = reportes.filter { it.resuelto }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (pendientes.isNotEmpty()) {
                                item { SeccionHeader("PENDIENTES", MaterialTheme.colorScheme.primary) }
                                items(pendientes, key = { it.id }) { reporte ->
                                    TarjetaReporte(
                                        reporte = reporte,
                                        onClick = onReporteClick,
                                        onDelete = { reporteAEliminar = it },
                                        onEdit = onEditReporte,
                                        onToggleStatus = onToggleStatus,
                                        onToggleFavorito = onToggleFavorito,
                                        onCopiar = onCopiar
                                    )
                                }
                            }
                            if (completados.isNotEmpty()) {
                                item { SeccionHeader("RESUELTOS", Color(0xFF00BFA5)) }
                                items(completados, key = { it.id }) { reporte ->
                                    TarjetaReporte(
                                        reporte = reporte,
                                        onClick = onReporteClick,
                                        onDelete = { reporteAEliminar = it },
                                        onEdit = onEditReporte,
                                        onToggleStatus = onToggleStatus,
                                        onToggleFavorito = onToggleFavorito,
                                        onCopiar = onCopiar
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    reporteAEliminar?.let { reporte ->
        AlertDialog(
            onDismissRequest = {
                triggerHaptic()
                reporteAEliminar = null
            },
            title = { Text("Confirmar eliminación") },
            text = { Text("¿Estás seguro de eliminar este elemento?") },
            confirmButton = {
                TextButton(onClick = {
                    triggerHaptic()
                    onDeleteReporte(reporte)
                    reporteAEliminar = null
                }) {
                    Text("Confirmar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    triggerHaptic()
                    reporteAEliminar = null
                }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun ResumenContadores(
    pendientes: Int,
    completados: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ContadorItem(
                etiqueta = "Pendientes",
                valor = pendientes,
                color = MaterialTheme.colorScheme.primary
            )
            VerticalDivider(modifier = Modifier.height(32.dp))
            ContadorItem(
                etiqueta = "Completados",
                valor = completados,
                color = Color(0xFF00BFA5)
            )
        }
    }
}

@Composable
private fun ContadorItem(etiqueta: String, valor: Int, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.semantics { contentDescription = "$etiqueta: $valor" }
    ) {
        Text(
            text = "$etiqueta: $valor",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun SeccionHeader(titulo: String, color: Color) {
    Text(
        text = titulo,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun EstadoInformativo(
    mensaje: String,
    accionTexto: String,
    onAccion: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Text(text = mensaje, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                onAccion()
            }) { Text(accionTexto) }
        }
    }
}
