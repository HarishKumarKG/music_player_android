package com.harish.mediaplayer.ui.brand

import com.harish.mediaplayer.ui.theme.LocalIsDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Brand palette (same as the launcher icon)
val BrandRed = Color(0xFFE63946)        // Strawberry Red  - accent, equalizer, play buttons

val BrandHoneydew = Color(0xFFF1FAEE)   // Honeydew        - light background, logo H

val BrandFrosted = Color(0xFFA8DADC)    // Frosted Blue    - soft containers, ring

val BrandSteel = Color(0xFF457B9D)      // Steel Blue      - primary

val BrandDeepSpace = Color(0xFF1D3557)  // Deep Space Blue - dark base, text

val BrandGradient = listOf(BrandDeepSpace, BrandSteel)

// Dark theme: ~70% blacks, ~25% graphite/gunmetal greys, ~5% Strawberry Red accents
val DarkInk = Color(0xFF0B0B0C)        // almost black (backgrounds)

val DarkCharcoal = Color(0xFF1C1C1E)   // raised black (cards, dialogs)

val DarkGraphite = Color(0xFF2C2C30)   // graphite (containers)

val DarkGunmetal = Color(0xFF3A3B40)   // gunmetal (highlights)

val DarkBrandGradient = listOf(DarkCharcoal, DarkGunmetal)

/** Badge/tile gradient that follows the theme: blues in light, black -> crimson in dark. */
@Composable
fun themedBrandGradient(): List<Color> =
    if (LocalIsDarkTheme.current) DarkBrandGradient else BrandGradient
