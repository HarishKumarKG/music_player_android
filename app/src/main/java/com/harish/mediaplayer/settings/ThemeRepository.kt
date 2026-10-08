package com.harish.mediaplayer.settings

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode(val label: String, val description: String) {
    SYSTEM("System default", "Follow the phone's light / dark setting"),
    LIGHT("Light", "Colourful pastel gradients"),
    DARK("Dark", "Deep night with glowing colours")
}

/** Stores the chosen theme in SharedPreferences and exposes it as a StateFlow. */
@Singleton
class ThemeRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(readThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME, mode.name).apply()
        _themeMode.value = mode // UI recomposes instantly
    }

    private fun readThemeMode(): ThemeMode =
        runCatching { ThemeMode.valueOf(prefs.getString(KEY_THEME, null) ?: "") }
            .getOrDefault(ThemeMode.SYSTEM)

    private companion object {
        const val KEY_THEME = "theme_mode"
    }
}
