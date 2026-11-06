package com.example.prscanner.data.repository

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Supported themes for PR Scanner:
 * - PREMIUM_BLUE: Executive royal sapphire and deep midnight navy
 * - WHITE: Crisp minimalist daylight with pure white elevated surfaces
 * - DARK: Stealth midnight obsidian with high-contrast slate surfaces
 */
enum class AppThemeMode(
    val id: String,
    val displayName: String,
    val subtitle: String
) {
    PREMIUM_BLUE(
        id = "premium_blue",
        displayName = "Premium Blue",
        subtitle = "Signature Royal Navy & Electric Sapphire"
    ),
    WHITE(
        id = "white",
        displayName = "Clean White",
        subtitle = "Crisp Minimalist Daylight & Pure White Surfaces"
    ),
    DARK(
        id = "dark",
        displayName = "Midnight Dark",
        subtitle = "Deep Stealth Obsidian & High-Contrast Slate"
    );

    companion object {
        fun fromId(id: String?): AppThemeMode {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: PREMIUM_BLUE
        }
    }
}

class ThemeRepository(context: Context) {
    private val prefs = context.getSharedPreferences("pr_theme_preferences", Context.MODE_PRIVATE)

    private val _currentTheme = MutableStateFlow(
        AppThemeMode.fromId(prefs.getString(KEY_APP_THEME, AppThemeMode.PREMIUM_BLUE.id))
    )
    val currentTheme: StateFlow<AppThemeMode> = _currentTheme.asStateFlow()

    fun setTheme(theme: AppThemeMode) {
        prefs.edit().putString(KEY_APP_THEME, theme.id).apply()
        _currentTheme.value = theme
    }

    companion object {
        private const val KEY_APP_THEME = "key_selected_app_theme_mode"
    }
}
