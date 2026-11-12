package com.example.labo_android_semana_02.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.labo_android_semana_02.domain.model.EstadoCurso
import com.example.labo_android_semana_02.domain.model.EstadoEntrega
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object UiFormatters {
    private val dateTimeFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale("es", "CO"))
    private val shortDateFormat = SimpleDateFormat("dd MMM yyyy", Locale("es", "CO"))

    fun formatearFechaHora(epochMs: Long): String = dateTimeFormat.format(Date(epochMs))

    fun formatearFechaCorta(epochMs: Long): String = shortDateFormat.format(Date(epochMs))

    fun formatearNota(nota: Double?): String =
        if (nota == null) "—" else String.format(Locale.US, "%.2f", nota)

    fun formatearTamano(bytes: Long?): String {
        if (bytes == null || bytes <= 0L) return ""
        val kb = bytes / 1024.0
        if (kb < 1024.0) return String.format(Locale.US, "%.0f KB", kb)
        val mb = kb / 1024.0
        return String.format(Locale.US, "%.2f MB", mb)
    }
}

@Composable
fun EstadoEntregaChip(estado: EstadoEntrega, modifier: Modifier = Modifier) {
    val (bg, fg, label) = when (estado) {
        EstadoEntrega.PENDIENTE -> Triple(Color(0xFFFFF3CD), Color(0xFF856404), "Pendiente")
        EstadoEntrega.ENTREGADA -> Triple(Color(0xFFD1ECF1), Color(0xFF0C5460), "Entregada")
        EstadoEntrega.TARDIA -> Triple(Color(0xFFF8D7DA), Color(0xFF842029), "Tardía")
        EstadoEntrega.CALIFICADA -> Triple(Color(0xFFD1E7DD), Color(0xFF0F5132), "Calificada")
        EstadoEntrega.EXENTA -> Triple(Color(0xFFE2D9F3), Color(0xFF432874), "Exenta")
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier,
    ) {
        Text(
            text = label,
            color = fg,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
fun EstadoCursoChip(estado: EstadoCurso, modifier: Modifier = Modifier) {
    val activo = estado == EstadoCurso.ACTIVO
    val bg = if (activo) Color(0xFFD1E7DD) else Color(0xFFE2E3E5)
    val fg = if (activo) Color(0xFF0F5132) else Color(0xFF41464B)
    Surface(
        color = bg,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier,
    ) {
        Text(
            text = if (activo) "Activo" else "Archivado",
            color = fg,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
fun BannerMensaje(
    texto: String,
    esError: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (esError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
    val fg = if (esError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer

    Surface(
        color = bg,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = texto,
                color = fg,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = fg, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
fun EstadoCargandoPantalla(mensaje: String = "Cargando...") {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(40.dp))
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = mensaje, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun EstadoVacioTarjeta(titulo: String, descripcion: String) {
    Surface(
        tonalElevation = 1.dp,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = descripcion,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun DialogoConfirmacion(
    titulo: String,
    mensaje: String,
    textoConfirmar: String = "Confirmar",
    esDestructivo: Boolean = true,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(titulo) },
        text = { Text(mensaje) },
        confirmButton = {
            Button(
                onClick = onConfirmar,
                colors = if (esDestructivo) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    )
                } else {
                    ButtonDefaults.buttonColors()
                },
            ) {
                Text(textoConfirmar)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) {
                Text("Cancelar")
            }
        },
    )
}

@Composable
fun WrapRow(
    modifier: Modifier = Modifier,
    horizontalSpacing: Dp = 8.dp,
    verticalSpacing: Dp = 8.dp,
    content: @Composable () -> Unit,
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val hGap = horizontalSpacing.roundToPx()
        val vGap = verticalSpacing.roundToPx()
        val maxWidth = if (constraints.hasBoundedWidth) constraints.maxWidth else Int.MAX_VALUE
        val childConstraints = constraints.copy(minWidth = 0, minHeight = 0)

        val rows = mutableListOf<List<Placeable>>()
        var currentRow = mutableListOf<Placeable>()
        var currentWidth = 0
        var totalHeight = 0
        var maxRowWidth = 0

        for (measurable in measurables) {
            val placeable = measurable.measure(childConstraints)
            val needed = if (currentRow.isEmpty()) placeable.width else placeable.width + hGap
            if (currentRow.isNotEmpty() && currentWidth + needed > maxWidth) {
                rows.add(currentRow)
                maxRowWidth = maxOf(maxRowWidth, currentWidth)
                totalHeight += (currentRow.maxOfOrNull { it.height } ?: 0) + vGap
                currentRow = mutableListOf(placeable)
                currentWidth = placeable.width
            } else {
                currentRow.add(placeable)
                currentWidth += needed
            }
        }
        if (currentRow.isNotEmpty()) {
            rows.add(currentRow)
            maxRowWidth = maxOf(maxRowWidth, currentWidth)
            totalHeight += currentRow.maxOfOrNull { it.height } ?: 0
        }

        val layoutWidth = if (constraints.hasBoundedWidth) {
            constraints.maxWidth
        } else {
            maxRowWidth.coerceAtLeast(constraints.minWidth)
        }
        val layoutHeight = totalHeight.coerceAtLeast(constraints.minHeight)

        layout(layoutWidth, layoutHeight) {
            var y = 0
            for (row in rows) {
                var x = 0
                val rowHeight = row.maxOfOrNull { it.height } ?: 0
                for (placeable in row) {
                    placeable.placeRelative(x = x, y = y + (rowHeight - placeable.height) / 2)
                    x += placeable.width + hGap
                }
                y += rowHeight + vGap
            }
        }
    }
}
