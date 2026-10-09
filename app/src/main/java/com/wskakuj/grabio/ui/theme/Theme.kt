package com.wskakuj.grabio.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Paleta z ikony: neonowa zieleń + głęboka czerń.
private val Neon = Color(0xFFA3FF12)
private val Ink = Color(0xFF0B0B0B)

private val LightColors = lightColorScheme(
    primary = Color(0xFF3B7A0F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6F5A6),
    onPrimaryContainer = Color(0xFF11230A),
    secondary = Color(0xFF55624C),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD9E7CC),
    onSecondaryContainer = Color(0xFF131F0D),
    tertiary = Color(0xFF386568),
    onTertiary = Color.White,
    background = Color(0xFFFBFDF7),
    onBackground = Color(0xFF191D16),
    surface = Color(0xFFFBFDF7),
    onSurface = Color(0xFF191D16),
    surfaceVariant = Color(0xFFE1E4D5),
    onSurfaceVariant = Color(0xFF44483D),
    outline = Color(0xFF757A6D),
    outlineVariant = Color(0xFFC5C8BA),
    error = Color(0xFFBA1A1A),
    onError = Color.White
)

private val DarkColors = darkColorScheme(
    primary = Neon,
    onPrimary = Color(0xFF153500),
    primaryContainer = Color(0xFF2A4A0A),
    onPrimaryContainer = Color(0xFFD6F5A6),
    secondary = Color(0xFFB4CC9E),
    onSecondary = Color(0xFF203616),
    secondaryContainer = Color(0xFF364E2B),
    onSecondaryContainer = Color(0xFFD0E8BA),
    tertiary = Color(0xFFA0CFD2),
    onTertiary = Color(0xFF00363A),
    background = Ink,
    onBackground = Color(0xFFE4E4DC),
    surface = Color(0xFF121212),
    onSurface = Color(0xFFE4E4DC),
    surfaceVariant = Color(0xFF262A20),
    onSurfaceVariant = Color(0xFFC5C8BA),
    outline = Color(0xFF8F9384),
    outlineVariant = Color(0xFF44483D),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

@Composable
fun GrabioTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
