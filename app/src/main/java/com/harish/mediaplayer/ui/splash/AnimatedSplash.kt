package com.harish.mediaplayer.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.harish.mediaplayer.ui.brand.BrandGradient
import com.harish.mediaplayer.ui.brand.DarkCharcoal
import com.harish.mediaplayer.ui.brand.DarkGraphite
import com.harish.mediaplayer.ui.brand.DarkInk
import com.harish.mediaplayer.ui.brand.HarishLogo
import com.harish.mediaplayer.ui.theme.LocalIsDarkTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Animated intro (~2s):
 *  ring draws itself -> the two legs of the "H" spring up -> crossbar slides out
 *  -> the crossbar turns into a live equalizer -> "Harish" fades/slides in.
 * The logo itself lives in HarishLogo so the songs screen can reuse it.
 */
@Composable
fun AnimatedSplash(onFinished: () -> Unit) {
    val title = remember { Animatable(0f) }
    val subtitle = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { delay(800); title.animateTo(1f, tween(650, easing = FastOutSlowInEasing)) }
        launch { delay(1100); subtitle.animateTo(1f, tween(500)) }
        delay(2200)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Light: brand blues. Dark: black -> graphite (the red equalizer is the only colour)
            .background(
                Brush.linearGradient(
                    if (LocalIsDarkTheme.current) listOf(DarkInk, DarkCharcoal, DarkGraphite) else BrandGradient
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            HarishLogo(modifier = Modifier.size(200.dp), loop = true)

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Harish",
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (14f - 10f * title.value).sp, // letters "close in" as it appears
                modifier = Modifier.graphicsLayer {
                    alpha = title.value
                    translationY = (1f - title.value) * 24.dp.toPx()
                }
            )
            Text(
                text = "MUSIC PLAYER",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 5.sp,
                modifier = Modifier.graphicsLayer { alpha = subtitle.value }
            )
        }
    }
}

@Preview
@Composable
private fun AnimatedSplashPreview() {
    AnimatedSplash(onFinished = {})
}
