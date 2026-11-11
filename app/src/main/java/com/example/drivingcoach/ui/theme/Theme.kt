package com.example.drivingcoach.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DayColors = lightColorScheme(
    primary = DayInk, onPrimary = DaySurface,
    primaryContainer = DayAccentSurface, onPrimaryContainer = DayAccent,
    secondary = DayAccent, onSecondary = Color.White,
    secondaryContainer = DaySoftSurface, onSecondaryContainer = DayInk,
    background = DayBackground, onBackground = DayInk,
    surface = DaySurface, onSurface = DayInk,
    surfaceVariant = DaySoftSurface, onSurfaceVariant = DaySecondaryInk,
    outline = Color(0xFF68746C), outlineVariant = Color(0xFFD9E0DA),
)

private val NightColors = darkColorScheme(
    primary = NightInk, onPrimary = NightBackground,
    primaryContainer = NightAccentSurface, onPrimaryContainer = NightAccent,
    secondary = NightAccent, onSecondary = NightBackground,
    secondaryContainer = NightSoftSurface, onSecondaryContainer = NightInk,
    background = NightBackground, onBackground = NightInk,
    surface = NightSurface, onSurface = NightInk,
    surfaceVariant = NightSoftSurface, onSurfaceVariant = NightSecondaryInk,
    outline = Color(0xFFA0ADA4), outlineVariant = Color(0xFF3A463E),
)

@Composable
fun DrivingCoachUITheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) NightColors else DayColors,
        typography = Typography,
        content = content,
    )
}