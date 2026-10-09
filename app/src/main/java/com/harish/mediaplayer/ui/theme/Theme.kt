package com.harish.mediaplayer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Palette: Strawberry Red #E63946, Honeydew #F1FAEE, Frosted Blue #A8DADC,
 *          Steel Blue #457B9D, Deep Space Blue #1D3557.
 * primary = blues (calm, readable), tertiary = Strawberry Red (the accent that pops).
 */
private val BrandLightColors = lightColorScheme(
    primary = Color(0xFF457B9D),            // Steel Blue
    onPrimary = Color.White,
    primaryContainer = Color(0xFFA8DADC),   // Frosted Blue (mini player)
    onPrimaryContainer = Color(0xFF1D3557),
    secondary = Color(0xFF1D3557),          // Deep Space Blue
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD4ECEC),
    onSecondaryContainer = Color(0xFF1D3557),
    tertiary = Color(0xFFE63946),           // Strawberry Red
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFCD9DC),
    onTertiaryContainer = Color(0xFF5C0A12),
    background = Color(0xFFF1FAEE),         // Honeydew
    onBackground = Color(0xFF1D3557),
    surface = Color(0xFFF1FAEE),
    onSurface = Color(0xFF1D3557),
    surfaceVariant = Color(0xFFDCEDEA),
    onSurfaceVariant = Color(0xFF4A6276),
    outline = Color(0xFF7A93A6),
    outlineVariant = Color(0xFFC9DEDD),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEAF6EA),
    surfaceContainer = Color(0xFFE3F1E6),
    surfaceContainerHigh = Color(0xFFDCEDE3),
    surfaceContainerHighest = Color(0xFFD3E7E0)
)

/**
 * Dark: ~70% blacks, ~25% graphite/gunmetal greys, ~5% Strawberry Red.
 * Red lives only in `tertiary` (hearts, play buttons, current song, progress) so it stays an accent.
 */
private val BrandDarkColors = darkColorScheme(
    primary = Color(0xFFE2E2E6),            // soft silver (tabs, section headers, radio buttons)
    onPrimary = Color(0xFF1C1C1E),
    primaryContainer = Color(0xFF2C2C30),   // graphite (mini player)
    onPrimaryContainer = Color(0xFFF1F1F1),
    secondary = Color(0xFFB9BAC0),
    onSecondary = Color(0xFF1C1C1E),
    secondaryContainer = Color(0xFF2A2A2E),
    onSecondaryContainer = Color(0xFFE8E8EC),
    tertiary = Color(0xFFE63946),           // Strawberry Red — the only red, used sparingly
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF3A1416),
    onTertiaryContainer = Color(0xFFFFD9DC),
    background = Color(0xFF0B0B0C),         // almost black
    onBackground = Color(0xFFF1F1F1),
    surface = Color(0xFF0B0B0C),
    onSurface = Color(0xFFF1F1F1),
    surfaceVariant = Color(0xFF1E1E21),
    onSurfaceVariant = Color(0xFFB3B4BA),
    outline = Color(0xFF7C7D84),
    outlineVariant = Color(0xFF2A2A2E),
    surfaceContainerLowest = Color(0xFF050505),
    surfaceContainerLow = Color(0xFF121214),  // drawer, bottom sheets
    surfaceContainer = Color(0xFF17171A),
    surfaceContainerHigh = Color(0xFF1C1C1F),  // dialogs, dropdown menus
    surfaceContainerHighest = Color(0xFF242428)
)

/** Lets any composable ask "are we in dark mode?" (the app may differ from the phone setting). */
val LocalIsDarkTheme = staticCompositionLocalOf { false }

@Composable
fun MediaPlayerTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    // Dynamic (wallpaper) colours are no longer used, so the brand colours always show
    CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = if (darkTheme) BrandDarkColors else BrandLightColors,
            typography = Typography,
            content = content
        )
    }
}
