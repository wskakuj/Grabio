package com.treningcheck.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF2E7D32),
    onPrimary = Color.White,
    secondary = Color(0xFF1565C0),
    tertiary = Color(0xFFEF6C00)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF81C784),
    secondary = Color(0xFF64B5F6),
    tertiary = Color(0xFFFFB74D)
)

@Composable
fun TreningCheckTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
