package com.harish.mediaplayer.ui.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.sin

/**
 * The "now playing" equalizer: 4 bars bouncing up and down like a music visualiser.
 * While paused they slowly settle to small, still bars, so you can still see which song
 * is the current one.
 *
 * Example: `PlayingBars(isPlaying = true, color = Color.White, modifier = Modifier.size(20.dp))`
 */
@Composable
fun PlayingBars(isPlaying: Boolean, color: Color, modifier: Modifier = Modifier, barCount: Int = 4) {
    val transition = rememberInfiniteTransition(label = "playingBars")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
        label = "phase"
    )
    // 1 = bouncing, 0 = resting (smooth change between the two)
    val motion by animateFloatAsState(if (isPlaying) 1f else 0f, tween(400), label = "motion")

    Canvas(modifier = modifier) {
        val gap = size.width * 0.12f
        val barWidth = (size.width - gap * (barCount - 1)) / barCount
        for (i in 0 until barCount) {
            // each bar has its own speed and offset, so they don't move in lockstep
            val wave = (sin(phase * (1f + i * 0.35f) + i * 1.7f) + 1f) / 2f // 0..1
            val resting = 0.25f
            val fraction = resting + (0.2f + 0.8f * wave - resting) * motion
            val barHeight = size.height * fraction.coerceIn(0.15f, 1f)
            drawRoundRect(
                color = color,
                topLeft = Offset(i * (barWidth + gap), size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2)
            )
        }
    }
}
