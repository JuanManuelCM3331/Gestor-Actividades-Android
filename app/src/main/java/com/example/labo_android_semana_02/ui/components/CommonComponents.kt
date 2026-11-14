package com.example.labo_android_semana_02.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.labo_android_semana_02.domain.model.EstadoCurso
import com.example.labo_android_semana_02.domain.model.EstadoEntrega
import com.example.labo_android_semana_02.ui.theme.coloresExtendidos
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object UiFormatters {
    private val localeEsCo = Locale.forLanguageTag("es-CO")
    private val dateTimeFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", localeEsCo)
    private val shortDateFormat = SimpleDateFormat("dd MMM yyyy", localeEsCo)

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

/** Insignia de color con icono, usada para estados. */
@Composable
private fun InsigniaEstado(
    texto: String,
    icono: ImageVector,
    contenedor: Color,
    contenido: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = contenedor,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, contenido.copy(alpha = 0.25f)),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(start = 8.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icono,
                contentDescription = null,
                tint = contenido,
                modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = texto,
                color = contenido,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
fun EstadoEntregaChip(estado: EstadoEntrega, modifier: Modifier = Modifier) {
    val c = MaterialTheme.coloresExtendidos
    when (estado) {
        EstadoEntrega.PENDIENTE ->
            InsigniaEstado("Pendiente", Icons.Filled.Schedule, c.advertenciaContenedor, c.advertencia, modifier)
        EstadoEntrega.ENTREGADA ->
            InsigniaEstado("Entregada", Icons.Filled.TaskAlt, c.informacionContenedor, c.informacion, modifier)
        EstadoEntrega.TARDIA ->
            InsigniaEstado(
                "Tardía",
                Icons.Filled.WarningAmber,
                MaterialTheme.colorScheme.errorContainer,
                MaterialTheme.colorScheme.error,
                modifier,
            )
        EstadoEntrega.CALIFICADA ->
            InsigniaEstado("Calificada", Icons.Filled.DoneAll, c.exitoContenedor, c.exito, modifier)
        EstadoEntrega.EXENTA ->
            InsigniaEstado("Exenta", Icons.Filled.RemoveCircleOutline, c.especialContenedor, c.especial, modifier)
    }
}

@Composable
fun EstadoCursoChip(estado: EstadoCurso, modifier: Modifier = Modifier) {
    val c = MaterialTheme.coloresExtendidos
    if (estado == EstadoCurso.ACTIVO) {
        InsigniaEstado("Activo", Icons.Filled.CheckCircle, c.exitoContenedor, c.exito, modifier)
    } else {
        InsigniaEstado("Archivado", Icons.Filled.Archive, c.neutroContenedor, c.neutro, modifier)
    }
}

/** Icono dentro de un cuadrado redondeado tintado (avatar de listas y tarjetas). */
@Composable
fun IconoTintado(
    icono: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    tamano: Dp = 44.dp,
    circular: Boolean = false,
) {
    Box(
        modifier = modifier
            .size(tamano)
            .clip(if (circular) CircleShape else RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icono,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(tamano * 0.52f),
        )
    }
}

/** Titulo de seccion con icono de color. */
@Composable
fun TituloConIcono(
    texto: String,
    icono: ImageVector,
    color: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icono,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = texto,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
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
    val c = MaterialTheme.coloresExtendidos
    val bg = if (esError) MaterialTheme.colorScheme.errorContainer else c.exitoContenedor
    val fg = if (esError) MaterialTheme.colorScheme.onErrorContainer else c.exito
    val acento = if (esError) MaterialTheme.colorScheme.error else c.exito

    Surface(
        color = bg,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, acento.copy(alpha = 0.35f)),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (esError) Icons.Filled.ErrorOutline else Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = acento,
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = texto,
                color = fg,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
            )
            AppIconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Cerrar mensaje",
                    tint = fg,
                    modifier = Modifier.size(18.dp),
                )
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
        CircularProgressIndicator(
            modifier = Modifier.size(40.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primaryContainer,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = mensaje,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun EstadoVacioTarjeta(
    titulo: String,
    descripcion: String,
    icono: ImageVector = Icons.Outlined.Inbox,
) {
    Surface(
        color = MaterialTheme.coloresExtendidos.superficieTarjeta,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
            IconoTintado(
                icono = icono,
                color = MaterialTheme.colorScheme.primary,
                tamano = 56.dp,
                circular = true,
            )
            Spacer(modifier = Modifier.height(12.dp))
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
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.coloresExtendidos.superficieTarjeta,
        icon = {
            Icon(
                imageVector = if (esDestructivo) Icons.Filled.Delete else Icons.Filled.TaskAlt,
                contentDescription = null,
                tint = if (esDestructivo) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            )
        },
        title = { Text(titulo, textAlign = TextAlign.Center) },
        text = { Text(mensaje) },
        confirmButton = {
            AppButton(
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
            AppTextButton(onClick = onCancelar) {
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
