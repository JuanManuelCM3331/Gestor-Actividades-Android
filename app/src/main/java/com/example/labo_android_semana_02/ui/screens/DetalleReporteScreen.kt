package com.example.labo_android_semana_02.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.labo_android_semana_02.domain.Reporte
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleReporteScreen(
    reporte: Reporte?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle de Reporte") },
                navigationIcon = {
                    TextButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        onBack()
                    }) { Text("< Volver") }
                }
            )
        }
    ) { paddingValues ->
        if (reporte == null) {
            Box(
                modifier = modifier.padding(paddingValues).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Reporte no encontrado")
            }
        } else {
            val fechaStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(reporte.fecha))
            Column(
                modifier = modifier
                    .padding(paddingValues)
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(reporte.titulo, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(reporte.descripcion.ifBlank { "Sin descripción" })
                Text("Fecha: $fechaStr", style = MaterialTheme.typography.bodyMedium)
                reporte.categoriaNombre?.let { cat ->
                    Text("Categoría: $cat", style = MaterialTheme.typography.bodyMedium)
                }
                Text(
                    text = "Estado: ${if (reporte.resuelto) "Resuelto" else "Pendiente"}",
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (reporte.sincronizado) "Sincronizado con MySQL" else "Pendiente de sincronizar (offline)",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (reporte.sincronizado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )

                if (!reporte.evidenciaFotoUri.isNullOrBlank()) {
                    Text("Evidencia Fotográfica:", fontWeight = FontWeight.Bold)
                    Text(reporte.evidenciaFotoUri, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
                if (!reporte.evidenciaArchivoUri.isNullOrBlank()) {
                    Text("Evidencia de Archivo:", fontWeight = FontWeight.Bold)
                    Text(reporte.evidenciaArchivoUri, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
