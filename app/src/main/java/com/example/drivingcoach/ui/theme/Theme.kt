package com.example.drivingcoach.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Basic600Light, onPrimary = Basic00Light,
    secondary = Basic600Light, onSecondary = Basic00Light,
    background = Basic50Light, onBackground = Basic900Light,
    surface = Basic00Light, onSurface = Basic900Light,
    surfaceVariant = Basic100Light, onSurfaceVariant = Basic600Light,
    outline = Basic600Light, outlineVariant = Basic100Light,
    inverseSurface = Basic900Light, inverseOnSurface = Basic00Light
)
private val DarkColors = darkColorScheme(
    primary = Basic600Dark, onPrimary = Basic00Dark,
    secondary = Basic600Dark, onSecondary = Basic00Dark,
    background = Basic50Dark, onBackground = Basic900Dark,
    surface = Basic50Dark, onSurface = Basic900Dark,
    surfaceVariant = Basic100Dark, onSurfaceVariant = Basic500Dark,
    outline = Basic500Dark, outlineVariant = Basic100Dark,
    inverseSurface = Basic900Dark, inverseOnSurface = Basic00Dark
)

@Composable
fun DrivingCoachUITheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Follow vehicle/system night mode; wallpaper-derived colors must not replace Pleos tokens.
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography, content = content)
}
