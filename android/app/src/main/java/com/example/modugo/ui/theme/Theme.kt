package com.example.modugo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Mint,
    onPrimary = Color(0xFF07372A),
    primaryContainer = Color(0xFF214A40),
    onPrimaryContainer = Color(0xFFC4F8E6),
    secondary = Color(0xFFA7D2FF),
    background = Ink,
    surface = Ink,
    surfaceVariant = Panel,
    onBackground = Color(0xFFF1F6F8),
    onSurface = Color(0xFFF1F6F8),
    onSurfaceVariant = Muted,
    outline = Color(0xFF7A8D99),
    error = Color(0xFFFFB4AB)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF08674F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBFF4E2),
    onPrimaryContainer = Color(0xFF07372A),
    secondary = Color(0xFF215E91),
    background = Color(0xFFF1F5F5),
    surface = Color(0xFFF1F5F5),
    surfaceVariant = Color(0xFFE0E9EC),
    onBackground = Ink,
    onSurface = Ink,
    onSurfaceVariant = Color(0xFF3D515E)
)

@Composable
fun MODUGOTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography, content = content)
}
