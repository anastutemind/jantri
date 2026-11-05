package com.anaskhan.jantri.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Palette borrowed from a printed jantri: deep green ink, gold rule lines, warm paper.
val JantriGreen = Color(0xFF0B3D2E)
val JantriGreenMid = Color(0xFF14563F)
val JantriGold = Color(0xFFC9A227)
val JantriGoldLight = Color(0xFFE8C558)
val JantriPaper = Color(0xFFFBF7EC)
val JantriInk = Color(0xFF13251D)
val JantriMuted = Color(0xFF6B7A72)

private val LightColors = lightColorScheme(
    primary = JantriGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE9E2),
    onPrimaryContainer = JantriGreen,
    secondary = JantriGold,
    onSecondary = Color(0xFF241C00),
    secondaryContainer = Color(0xFFF6E9BE),
    onSecondaryContainer = Color(0xFF3E3000),
    background = JantriPaper,
    onBackground = JantriInk,
    surface = Color.White,
    onSurface = JantriInk,
    surfaceVariant = Color(0xFFEEEAE0),
    onSurfaceVariant = JantriMuted,
    outline = Color(0xFFC9C4B4),
    error = Color(0xFFB3261E),
)

private val DarkColors = darkColorScheme(
    primary = JantriGoldLight,
    onPrimary = Color(0xFF1A1A00),
    primaryContainer = JantriGreenMid,
    onPrimaryContainer = Color(0xFFDCEDE4),
    secondary = JantriGold,
    onSecondary = Color(0xFF241C00),
    secondaryContainer = Color(0xFF3E3000),
    onSecondaryContainer = Color(0xFFF6E9BE),
    background = Color(0xFF0E1512),
    onBackground = Color(0xFFE6E9E5),
    surface = Color(0xFF152019),
    onSurface = Color(0xFFE6E9E5),
    surfaceVariant = Color(0xFF22302A),
    onSurfaceVariant = Color(0xFFAFBAB4),
    outline = Color(0xFF44524B),
)

private val JantriTypography = Typography(
    displaySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 34.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 26.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 16.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp, lineHeight = 21.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 14.sp, letterSpacing = 0.4.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.8.sp),
)

@Composable
fun JantriTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = JantriTypography,
        content = content,
    )
}
