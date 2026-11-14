package com.example.labo_android_semana_02.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.example.labo_android_semana_02.ui.theme.coloresExtendidos

/*
 * Controles con sensacion fisica: escala suave (spring) mientras se presiona
 * y un toque haptico corto al activar.
 */

@Composable
private fun rememberFuente(): MutableInteractionSource = remember { MutableInteractionSource() }

@Composable
private fun escalaPresion(fuente: MutableInteractionSource, escalaMinima: Float = 0.96f): Float {
    val presionado by fuente.collectIsPressedAsState()
    val escala by animateFloatAsState(
        targetValue = if (presionado) escalaMinima else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "escalaPresion",
    )
    return escala
}

@Composable
private fun conHaptico(accion: () -> Unit): () -> Unit {
    val haptic = LocalHapticFeedback.current
    return {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        accion()
    }
}

@Composable
private fun IconoInicial(icono: ImageVector?) {
    if (icono != null) {
        Icon(imageVector = icono, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
    }
}

@Composable
fun AppButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    content: @Composable RowScope.() -> Unit,
) {
    val fuente = rememberFuente()
    Button(
        onClick = conHaptico(onClick),
        modifier = modifier.scale(escalaPresion(fuente)),
        enabled = enabled,
        colors = colors,
        interactionSource = fuente,
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 0.dp),
    ) {
        IconoInicial(icon)
        content()
    }
}

@Composable
fun AppTonalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    content: @Composable RowScope.() -> Unit,
) {
    AppButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        icon = icon,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
        content = content,
    )
}

@Composable
fun AppOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val fuente = rememberFuente()
    OutlinedButton(
        onClick = conHaptico(onClick),
        modifier = modifier.scale(escalaPresion(fuente)),
        enabled = enabled,
        interactionSource = fuente,
        border = BorderStroke(
            width = 1.2.dp,
            color = if (enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        IconoInicial(icon)
        content()
    }
}

@Composable
fun AppTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val fuente = rememberFuente()
    TextButton(
        onClick = conHaptico(onClick),
        modifier = modifier.scale(escalaPresion(fuente, 0.95f)),
        enabled = enabled,
        interactionSource = fuente,
    ) {
        IconoInicial(icon)
        content()
    }
}

@Composable
fun AppIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val fuente = rememberFuente()
    IconButton(
        onClick = conHaptico(onClick),
        modifier = modifier.scale(escalaPresion(fuente, 0.88f)),
        enabled = enabled,
        interactionSource = fuente,
        colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary),
        content = content,
    )
}

@Composable
fun AppFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val fuente = rememberFuente()
    FloatingActionButton(
        onClick = conHaptico(onClick),
        modifier = modifier.scale(escalaPresion(fuente, 0.92f)),
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        interactionSource = fuente,
        content = content,
    )
}

@Composable
fun AppFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    label: @Composable () -> Unit,
) {
    val fuente = rememberFuente()
    val contenedor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        label = "chipContenedor",
    )
    FilterChip(
        selected = selected,
        onClick = conHaptico(onClick),
        label = label,
        modifier = modifier.scale(escalaPresion(fuente, 0.95f)),
        interactionSource = fuente,
        leadingIcon = if (selected || icon != null) {
            {
                Icon(
                    imageVector = if (selected) Icons.Filled.Check else icon!!,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                )
            }
        } else {
            null
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = contenedor,
            selectedContainerColor = contenedor,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
            iconColor = MaterialTheme.colorScheme.primary,
        ),
    )
}

@Composable
fun AppAssistChip(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    label: @Composable () -> Unit,
) {
    val fuente = rememberFuente()
    AssistChip(
        onClick = conHaptico(onClick),
        label = label,
        modifier = modifier.scale(escalaPresion(fuente, 0.95f)),
        interactionSource = fuente,
        leadingIcon = icon?.let {
            {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.size(AssistChipDefaults.IconSize),
                )
            }
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f),
            labelColor = MaterialTheme.colorScheme.onTertiaryContainer,
            leadingIconContentColor = MaterialTheme.colorScheme.tertiary,
        ),
    )
}

/** Color de icono segun el tipo de archivo, armonizado con la paleta. */
@Composable
fun colorParaTipoArchivo(nombre: String, mime: String? = null): Color {
    val ext = nombre.substringAfterLast('.', "").lowercase()
    val m = mime.orEmpty().lowercase()
    val c = MaterialTheme.coloresExtendidos
    return when {
        ext == "pdf" || m.contains("pdf") -> MaterialTheme.colorScheme.error
        ext in listOf("png", "jpg", "jpeg", "gif", "webp") || m.startsWith("image/") -> c.exito
        ext in listOf("doc", "docx", "odt", "txt") || m.contains("word") -> c.informacion
        ext in listOf("xls", "xlsx", "csv", "ods") || m.contains("sheet") || m.contains("excel") -> c.exito
        ext in listOf("ppt", "pptx", "odp") || m.contains("presentation") -> c.advertencia
        ext in listOf("zip", "rar", "7z") || m.contains("zip") -> c.especial
        else -> MaterialTheme.colorScheme.primary
    }
}
