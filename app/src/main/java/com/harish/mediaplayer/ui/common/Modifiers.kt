package com.harish.mediaplayer.ui.common

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Makes this layer "catch" every touch on its whole area, so taps and drags on empty space
 * can't fall through to the screen drawn underneath it.
 *
 * Why it's needed: in Compose, when a layer on top has nothing touchable at the spot you tap,
 * the touch goes to whatever is below. A full-screen overlay without this lets you press
 * hidden buttons of the screen behind it.
 *
 * It only listens and never consumes events, so buttons, sliders and swipes inside the layer
 * keep working normally.
 */
fun Modifier.blockTouchesBehind(): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) awaitPointerEvent() // being a touch target is enough to stop fall-through
    }
}
