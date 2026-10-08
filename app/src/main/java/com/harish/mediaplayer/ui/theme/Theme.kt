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

private val BrandDarkColors = darkColorScheme(
    primary = Color(0xFFA8DADC),            // Frosted Blue
    onPrimary = Color(0xFF1D3557),
    primaryContainer = Color(0xFF2C5374),   // between Steel and Deep Space (mini player)
    onPrimaryContainer = Color(0xFFF1FAEE),
    secondary = Color(0xFF8FB8CF),
    onSecondary = Color(0xFF10223A),
    secondaryContainer = Color(0xFF2A4A66),
    onSecondaryContainer = Color(0xFFDDEEF2),
    tertiary = Color(0xFFE63946),           // Strawberry Red
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF7A1820),
    onTertiaryContainer = Color(0xFFFFD9DC),
    background = Color(0xFF0E1A2B),         // deeper than Deep Space Blue
    onBackground = Color(0xFFF1FAEE),
    surface = Color(0xFF0E1A2B),
    onSurface = Color(0xFFF1FAEE),
    surfaceVariant = Color(0xFF22344B),
    onSurfaceVariant = Color(0xFFB5CBD3),
    outline = Color(0xFF7F97A8),
    outlineVariant = Color(0xFF2E4560),
    surfaceContainerLowest = Color(0xFF09121F),
    surfaceContainerLow = Color(0xFF132238),
    surfaceContainer = Color(0xFF172840),
    surfaceContainerHigh = Color(0xFF1D3557), // Deep Space Blue (dialogs, menus)
    surfaceContainerHighest = Color(0xFF25406A)
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
