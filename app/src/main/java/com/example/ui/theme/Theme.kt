package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
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
import com.example.prscanner.data.repository.AppThemeMode

// ==========================================
// 1. Signature Premium Blue Scheme
// ==========================================
val PremiumBlueColorScheme = darkColorScheme(
    primary = PrPremiumBluePrimary,
    onPrimary = PrPremiumBlueOnPrimary,
    primaryContainer = PrPremiumBluePrimaryContainer,
    onPrimaryContainer = PrPremiumBlueOnPrimaryContainer,
    secondary = PrPremiumBlueSecondary,
    onSecondary = PrPremiumBlueBackground,
    background = PrPremiumBlueBackground,
    surface = PrPremiumBlueSurface,
    onBackground = PrPremiumBlueOnBackground,
    onSurface = PrPremiumBlueOnSurface,
    surfaceVariant = PrPremiumBlueSurfaceVariant,
    onSurfaceVariant = PrPremiumBlueOnSurfaceVariant,
    outline = PrPremiumBlueOutline,
    error = PrError
)

// ==========================================
// 2. Clean White / Daylight Scheme
// ==========================================
val WhiteColorScheme = lightColorScheme(
    primary = PrWhitePrimary,
    onPrimary = PrWhiteOnPrimary,
    primaryContainer = PrWhitePrimaryContainer,
    onPrimaryContainer = PrWhiteOnPrimaryContainer,
    secondary = PrWhiteSecondary,
    onSecondary = PrWhiteOnPrimary,
    background = PrWhiteBackground,
    surface = PrWhiteSurface,
    onBackground = PrWhiteOnBackground,
    onSurface = PrWhiteOnSurface,
    surfaceVariant = PrWhiteSurfaceVariant,
    onSurfaceVariant = PrWhiteOnSurfaceVariant,
    outline = PrWhiteOutline,
    error = PrError
)

// ==========================================
// 3. Midnight Dark / Stealth Scheme
// ==========================================
val DarkColorScheme = darkColorScheme(
    primary = PrDarkPrimary,
    onPrimary = PrDarkOnPrimary,
    primaryContainer = PrDarkPrimaryContainer,
    onPrimaryContainer = PrDarkOnPrimaryContainer,
    secondary = PrDarkSecondary,
    onSecondary = PrDarkBackground,
    background = PrDarkBackground,
    surface = PrDarkSurface,
    onBackground = PrDarkOnBackground,
    onSurface = PrDarkOnSurface,
    surfaceVariant = PrDarkSurfaceVariant,
    onSurfaceVariant = PrDarkOnSurfaceVariant,
    outline = PrDarkOutline,
    error = PrError
)

@Composable
fun MyApplicationTheme(
    themeMode: AppThemeMode = AppThemeMode.PREMIUM_BLUE,
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode) {
        AppThemeMode.PREMIUM_BLUE -> PremiumBlueColorScheme
        AppThemeMode.WHITE -> WhiteColorScheme
        AppThemeMode.DARK -> DarkColorScheme
    }

    val isLightSystemBars = themeMode == AppThemeMode.WHITE

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = isLightSystemBars
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
