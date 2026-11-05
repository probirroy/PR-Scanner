package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PrBlueAccent,
    onPrimary = PrCardLight,
    primaryContainer = PrNavyPrimary,
    onPrimaryContainer = PrCyanAccent,
    secondary = PrCyanAccent,
    onSecondary = PrNavyDark,
    background = PrSurfaceDark,
    surface = PrCardDark,
    onBackground = PrTextPrimaryDark,
    onSurface = PrTextPrimaryDark,
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = PrTextSecondaryDark,
    error = PrError
)

private val LightColorScheme = lightColorScheme(
    primary = PrNavyPrimary,
    onPrimary = PrCardLight,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = PrNavyPrimary,
    secondary = PrBlueAccent,
    onSecondary = PrCardLight,
    background = PrSurfaceLight,
    surface = PrCardLight,
    onBackground = PrTextPrimaryLight,
    onSurface = PrTextPrimaryLight,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = PrTextSecondaryLight,
    error = PrError
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
