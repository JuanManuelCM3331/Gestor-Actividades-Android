package com.example.labo_android_semana_02.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = OscuroPrimary,
    onPrimary = OscuroOnPrimary,
    primaryContainer = OscuroPrimaryContainer,
    onPrimaryContainer = OscuroOnPrimaryContainer,
    secondary = OscuroSecondary,
    onSecondary = OscuroOnSecondary,
    secondaryContainer = OscuroSecondaryContainer,
    onSecondaryContainer = OscuroOnSecondaryContainer,
    tertiary = OscuroTertiary,
    onTertiary = OscuroOnTertiary,
    tertiaryContainer = OscuroTertiaryContainer,
    onTertiaryContainer = OscuroOnTertiaryContainer,
    background = OscuroBackground,
    onBackground = OscuroOnBackground,
    surface = OscuroSurface,
    onSurface = OscuroOnSurface,
    surfaceVariant = OscuroSurfaceVariant,
    onSurfaceVariant = OscuroOnSurfaceVariant,
    outline = OscuroOutline,
    outlineVariant = OscuroOutlineVariant,
    error = OscuroError,
    onError = OscuroOnError,
    errorContainer = OscuroErrorContainer,
    onErrorContainer = OscuroOnErrorContainer,
)

private val LightColorScheme = lightColorScheme(
    primary = ClaroPrimary,
    onPrimary = ClaroOnPrimary,
    primaryContainer = ClaroPrimaryContainer,
    onPrimaryContainer = ClaroOnPrimaryContainer,
    secondary = ClaroSecondary,
    onSecondary = ClaroOnSecondary,
    secondaryContainer = ClaroSecondaryContainer,
    onSecondaryContainer = ClaroOnSecondaryContainer,
    tertiary = ClaroTertiary,
    onTertiary = ClaroOnTertiary,
    tertiaryContainer = ClaroTertiaryContainer,
    onTertiaryContainer = ClaroOnTertiaryContainer,
    background = ClaroBackground,
    onBackground = ClaroOnBackground,
    surface = ClaroCardSurface,
    onSurface = ClaroOnSurface,
    surfaceVariant = ClaroSurfaceVariant,
    onSurfaceVariant = ClaroOnSurfaceVariant,
    outline = ClaroOutline,
    outlineVariant = ClaroOutlineVariant,
    error = ClaroError,
    onError = ClaroOnError,
    errorContainer = ClaroErrorContainer,
    onErrorContainer = ClaroOnErrorContainer,
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

private val LocalColoresExtendidos = staticCompositionLocalOf { ColoresExtendidosClaro }

/** Acceso a los colores semanticos: `MaterialTheme.coloresExtendidos.exito`. */
val MaterialTheme.coloresExtendidos: ColoresExtendidos
    @Composable
    @ReadOnlyComposable
    get() = LocalColoresExtendidos.current

@Composable
fun Labo_android_semana_02Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendidos = if (darkTheme) ColoresExtendidosOscuro else ColoresExtendidosClaro

    CompositionLocalProvider(LocalColoresExtendidos provides extendidos) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
    }
}
