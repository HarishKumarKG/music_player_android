package com.harish.mediaplayer.ui.brand

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.harish.mediaplayer.ui.theme.LocalIsDarkTheme

/** Big, heavy title filled with the brand gradient and a soft coloured glow. */
@Composable
fun GradientTitle(text: String, modifier: Modifier = Modifier, fontSize: TextUnit = 30.sp) {
    // Darker end of the gradient on light backgrounds, brighter end on dark ones (keeps contrast)
    val colors = if (LocalIsDarkTheme.current) listOf(BrandHoneydew, BrandFrosted)
                 else listOf(BrandDeepSpace, BrandSteel)
    Text(
        text = text,
        modifier = modifier,
        style = TextStyle(
            brush = Brush.linearGradient(colors),
            fontSize = fontSize,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp,
            shadow = Shadow(
                color = BrandRed.copy(alpha = 0.30f), // subtle red glow
                offset = Offset(0f, 4f),
                blurRadius = 10f
            )
        )
    )
}
