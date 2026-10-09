package com.harish.mediaplayer.ui.brand

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The white "H + sound wave" logo, drawn in the launcher icon's 108x108 coordinates.
 * It animates in every time it enters the screen:
 *   ring draws -> H legs spring up -> crossbar slides out -> equalizer bars jump.
 *
 * @param loop true (splash): equalizer keeps bouncing and a glow pulses forever.
 *             false (header): equalizer bounces for a moment, then settles still.
 */
@Composable
fun HarishLogo(modifier: Modifier = Modifier, loop: Boolean) {
    val ring = remember { Animatable(0f) }
    val legs = remember { Animatable(0f) }
    val crossbar = remember { Animatable(0f) }
    val wave = remember { Animatable(0f) }
    val oneShotPhase = remember { Animatable(0f) }
    val settle = remember { Animatable(0f) } // 0 = bouncing, 1 = resting at full height

    LaunchedEffect(Unit) {
        launch { ring.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
        launch {
            delay(150)
            legs.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow))
        }
        launch { delay(450); crossbar.animateTo(1f, tween(350, easing = FastOutSlowInEasing)) }
        launch { delay(700); wave.animateTo(1f, tween(350, easing = FastOutSlowInEasing)) }
        if (!loop) {
            launch { delay(700); oneShotPhase.animateTo((3 * PI).toFloat(), tween(1200, easing = LinearEasing)) }
            launch { delay(1500); settle.animateTo(1f, tween(450, easing = FastOutSlowInEasing)) }
        }
    }

    // `loop` never changes for a given call site, so this branch is safe in Compose
    val phase: State<Float>
    val glow: State<Float>
    if (loop) {
        val infinite = rememberInfiniteTransition(label = "logo")
        phase = infinite.animateFloat(
            initialValue = 0f,
            targetValue = (2 * PI).toFloat(),
            animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing)),
            label = "eqPhase"
        )
        glow = infinite.animateFloat(
            initialValue = 0.20f,
            targetValue = 0.45f,
            animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
            label = "glow"
        )
    } else {
        phase = oneShotPhase.asState()
        glow = remember { mutableFloatStateOf(0f) }
    }

    Canvas(modifier = modifier) {
        val s = size.minDimension / 108f // 1 icon unit in pixels

        if (glow.value > 0f) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = glow.value * ring.value), Color.Transparent),
                    center = center,
                    radius = size.minDimension / 2
                ),
                radius = size.minDimension / 2
            )
        }

        // Ring that draws itself clockwise from the top
        drawArc(
            color = BrandFrosted.copy(alpha = 0.6f),
            startAngle = -90f,
            sweepAngle = 360f * ring.value,
            useCenter = false,
            topLeft = Offset(25 * s, 25 * s),
            size = Size(58 * s, 58 * s),
            style = Stroke(width = 1.6f * s, cap = StrokeCap.Round)
        )

        // Legs of the H grow from the middle outwards (with a small spring overshoot)
        val legHeight = 44 * s * legs.value
        if (legHeight > 0f) {
            for (x in floatArrayOf(35f, 65f)) {
                drawRoundRect(
                    color = BrandHoneydew,
                    topLeft = Offset(x * s, 54 * s - legHeight / 2),
                    size = Size(8 * s, legHeight),
                    cornerRadius = CornerRadius(4 * s)
                )
            }
        }

        // Crossbar slides out from the centre
        val barWidth = 24 * s * crossbar.value
        drawRect(
            color = BrandHoneydew,
            topLeft = Offset(54 * s - barWidth / 2, 52.4f * s),
            size = Size(barWidth, 3.2f * s)
        )

        // Equalizer bars jump out of the crossbar
        val centers = floatArrayOf(46.5f, 50f, 54f, 58f, 61.5f)
        val heights = floatArrayOf(8f, 14f, 24f, 14f, 8f)
        for (i in centers.indices) {
            val bounce = 0.5f + 0.5f * ((sin(phase.value + i * 0.9f) + 1f) / 2f) // 0.5..1.0
            val factor = bounce + (1f - bounce) * settle.value                    // ease to 1.0
            val h = heights[i] * s * factor * wave.value
            if (h > 0f) {
                drawRoundRect(
                    color = BrandRed, // the "music" pops in Strawberry Red
                    topLeft = Offset((centers[i] - 1.4f) * s, 54 * s - h / 2),
                    size = Size(2.8f * s, h),
                    cornerRadius = CornerRadius(1.4f * s)
                )
            }
        }
    }
}
