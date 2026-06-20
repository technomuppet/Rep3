package com.replog.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColors = darkColorScheme(primary = RepLogGreen, secondary = RepLogBlue, tertiary = RepLogOrange, background = RepLogBackground, surface = RepLogSurface, surfaceVariant = RepLogSurfaceVariant, onPrimary = RepLogBackground, onSecondary = RepLogTextPrimary, onTertiary = RepLogTextPrimary, onBackground = RepLogTextPrimary, onSurface = RepLogTextPrimary, onSurfaceVariant = RepLogTextSecondary, error = RepLogRed)
private val LightColors = lightColorScheme(primary = RepLogGreenDark, secondary = RepLogBlue, tertiary = RepLogOrange, background = RepLogLightBackground, surface = RepLogLightSurface, surfaceVariant = Color(0xFFE2E8F0), onPrimary = Color.White, onSecondary = Color.White, onTertiary = Color.White, onBackground = RepLogLightTextPrimary, onSurface = RepLogLightTextPrimary, onSurfaceVariant = RepLogLightTextSecondary, error = RepLogRed)

@Composable
fun RepLogTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) SideEffect {
        val window = (view.context as Activity).window
        window.statusBarColor = colors.background.toArgb(); window.navigationBarColor = colors.background.toArgb()
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
    }
    MaterialTheme(colorScheme = colors, typography = Typography, content = content)
}
