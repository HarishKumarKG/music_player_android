package com.harish.mediaplayer.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * Full-screen colourful backdrop.
 *  Light: Honeydew wash with Frosted Blue, a whisper of Strawberry Red and Steel Blue glows.
 *  Dark:  near-black with soft graphite / gunmetal glows and just a hint of Strawberry Red.
 */
@Composable
fun BrandBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val dark = LocalIsDarkTheme.current
    Box(
        modifier = modifier.drawBehind { if (dark) drawDarkBackdrop() else drawLightBackdrop() },
        content = content
    )
}

internal fun DrawScope.drawLightBackdrop() {
    // Honeydew base fading into a hint of Frosted Blue
    drawRect(
        Brush.verticalGradient(
            listOf(Color(0xFFF1FAEE), Color(0xFFEAF6F1), Color(0xFFE2F1F2))
        )
    )
    glow(Color(0xFFA8DADC), Offset(size.width * 0.05f, size.height * 0.08f), size.width * 0.95f, 0.85f) // Frosted
    glow(Color(0xFFE63946), Offset(size.width * 1.0f, size.height * 0.45f), size.width * 0.8f, 0.14f)   // Strawberry
    glow(Color(0xFF457B9D), Offset(size.width * 0.1f, size.height * 0.98f), size.width * 0.9f, 0.22f)   // Steel
}

internal fun DrawScope.drawDarkBackdrop() {
    // Mostly black...
    drawRect(
        Brush.verticalGradient(
            listOf(Color(0xFF060606), Color(0xFF0A0A0B), Color(0xFF0D0D0F))
        )
    )
    // ...with soft graphite / gunmetal light, and only a whisper of red
    glow(Color(0xFF3A3B40), Offset(size.width * 0.0f, size.height * 0.04f), size.width * 1.0f, 0.55f)  // gunmetal
    glow(Color(0xFF2C2C30), Offset(size.width * 0.25f, size.height * 1.0f), size.width * 0.9f, 0.6f)   // graphite
    glow(Color(0xFFE63946), Offset(size.width * 1.05f, size.height * 0.5f), size.width * 0.7f, 0.06f)  // red hint
}

/** Soft radial light that fades to transparent. */
private fun DrawScope.glow(color: Color, center: Offset, radius: Float, alpha: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = alpha), Color.Transparent),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )
}
