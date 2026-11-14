package com.example.labo_android_semana_02.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------------------------
// Paleta base del proyecto (Arena, Terracota y Cafe)
// ---------------------------------------------------------------------------
val PaletaNiebla = Color(0xFFEBEFEE)
val PaletaArena = Color(0xFFCCB499)
val PaletaTerracotaClara = Color(0xFFC8906D)
val PaletaTerracota = Color(0xFFBB6C43)
val PaletaCafe = Color(0xFF4A413C)

// ---------------------------------------------------------------------------
// Tema claro: fondo niebla, texto cafe, acento terracota (contraste >= 4.5:1)
// ---------------------------------------------------------------------------
val ClaroPrimary = Color(0xFFA65A33)
val ClaroOnPrimary = Color(0xFFFFFFFF)
val ClaroPrimaryContainer = Color(0xFFF6DFD0)
val ClaroOnPrimaryContainer = Color(0xFF4A2410)
val ClaroSecondary = Color(0xFF7A6650)
val ClaroOnSecondary = Color(0xFFFFFFFF)
val ClaroSecondaryContainer = Color(0xFFEADCCB)
val ClaroOnSecondaryContainer = Color(0xFF3B2F22)
val ClaroTertiary = Color(0xFF2F6F7E)
val ClaroOnTertiary = Color(0xFFFFFFFF)
val ClaroTertiaryContainer = Color(0xFFD3E8EC)
val ClaroOnTertiaryContainer = Color(0xFF123740)
val ClaroBackground = PaletaNiebla
val ClaroOnBackground = Color(0xFF332B27)
val ClaroSurface = Color(0xFFF4F6F5)
val ClaroOnSurface = Color(0xFF332B27)
val ClaroSurfaceVariant = Color(0xFFE6E0D9)
val ClaroOnSurfaceVariant = Color(0xFF675C55)
val ClaroOutline = Color(0xFF9B8F86)
val ClaroOutlineVariant = Color(0xFFD8CEC3)
val ClaroError = Color(0xFFB0372D)
val ClaroOnError = Color(0xFFFFFFFF)
val ClaroErrorContainer = Color(0xFFF8DCD7)
val ClaroOnErrorContainer = Color(0xFF55130E)
val ClaroCardSurface = Color(0xFFFBF9F6)

// ---------------------------------------------------------------------------
// Tema oscuro: base cafe calido, sin negro puro, para reducir la fatiga visual
// ---------------------------------------------------------------------------
val OscuroPrimary = Color(0xFFE2A381)
val OscuroOnPrimary = Color(0xFF3F1F0D)
val OscuroPrimaryContainer = Color(0xFF7A4326)
val OscuroOnPrimaryContainer = Color(0xFFFADFCF)
val OscuroSecondary = PaletaArena
val OscuroOnSecondary = Color(0xFF3B2F22)
val OscuroSecondaryContainer = Color(0xFF574A3F)
val OscuroOnSecondaryContainer = Color(0xFFEFE2D2)
val OscuroTertiary = Color(0xFF8FC7D3)
val OscuroOnTertiary = Color(0xFF0E333C)
val OscuroTertiaryContainer = Color(0xFF2C525B)
val OscuroOnTertiaryContainer = Color(0xFFD3EEF3)
val OscuroBackground = Color(0xFF2A2421)
val OscuroOnBackground = Color(0xFFE8EAE8)
val OscuroSurface = Color(0xFF312A27)
val OscuroOnSurface = Color(0xFFE8EAE8)
val OscuroSurfaceVariant = PaletaCafe
val OscuroOnSurfaceVariant = Color(0xFFCFC4BB)
val OscuroOutline = Color(0xFF8F8279)
val OscuroOutlineVariant = Color(0xFF5B514B)
val OscuroError = Color(0xFFF2A79D)
val OscuroOnError = Color(0xFF55130E)
val OscuroErrorContainer = Color(0xFF6B2A24)
val OscuroOnErrorContainer = Color(0xFFFADAD5)
val OscuroCardSurface = Color(0xFF3A322E)

/**
 * Colores semanticos armonizados con la paleta (exito, advertencia, informacion
 * y especial). Cada par tiene variante clara y oscura.
 */
@Immutable
data class ColoresExtendidos(
    val exito: Color,
    val exitoContenedor: Color,
    val advertencia: Color,
    val advertenciaContenedor: Color,
    val informacion: Color,
    val informacionContenedor: Color,
    val especial: Color,
    val especialContenedor: Color,
    val neutro: Color,
    val neutroContenedor: Color,
    val superficieTarjeta: Color,
)

val ColoresExtendidosClaro = ColoresExtendidos(
    exito = Color(0xFF3C7A55),
    exitoContenedor = Color(0xFFDCEBDF),
    advertencia = Color(0xFF9A6A00),
    advertenciaContenedor = Color(0xFFF5E6BF),
    informacion = Color(0xFF2D6A8C),
    informacionContenedor = Color(0xFFD7E8F1),
    especial = Color(0xFF7A4E8C),
    especialContenedor = Color(0xFFE9DDEF),
    neutro = Color(0xFF675C55),
    neutroContenedor = Color(0xFFE6E0D9),
    superficieTarjeta = ClaroCardSurface,
)

val ColoresExtendidosOscuro = ColoresExtendidos(
    exito = Color(0xFF8FCBA3),
    exitoContenedor = Color(0xFF2F4A3B),
    advertencia = Color(0xFFE8C16C),
    advertenciaContenedor = Color(0xFF55442A),
    informacion = Color(0xFF8CC4DF),
    informacionContenedor = Color(0xFF2C4757),
    especial = Color(0xFFD2B0DF),
    especialContenedor = Color(0xFF47344F),
    neutro = Color(0xFFCFC4BB),
    neutroContenedor = Color(0xFF50463F),
    superficieTarjeta = OscuroCardSurface,
)
