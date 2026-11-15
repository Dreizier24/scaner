package ru.cora.scanner.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = KoraBurgundy,
    onPrimary = Color.White,
    primaryContainer = KoraGoldSoft,
    onPrimaryContainer = KoraInk,
    secondary = KoraGold,
    onSecondary = KoraInk,
    background = KoraCream,
    onBackground = KoraInk,
    surface = Color.White,
    onSurface = KoraInk,
    surfaceVariant = Color(0xFFEFE8E0),
    onSurfaceVariant = KoraMuted,
    error = KoraError,
)

private val DarkColors = darkColorScheme(
    primary = KoraGold,
    onPrimary = KoraInk,
    primaryContainer = KoraBurgundyDark,
    onPrimaryContainer = KoraCream,
    secondary = KoraGoldSoft,
    onSecondary = KoraInk,
    background = Color(0xFF141012),
    onBackground = KoraCream,
    surface = Color(0xFF1F1A1C),
    onSurface = KoraCream,
    surfaceVariant = Color(0xFF2B2427),
    onSurfaceVariant = Color(0xFFCFC6CA),
    error = KoraError,
)

@Composable
fun KoraScannerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = KoraTypography,
        content = content,
    )
}
