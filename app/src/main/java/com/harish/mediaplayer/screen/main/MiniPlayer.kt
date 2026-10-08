package com.harish.mediaplayer.screen.main

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.harish.mediaplayer.R
import com.harish.mediaplayer.screen.main.model.Song
import com.harish.mediaplayer.util.PlaybackState
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Floating "now playing" card pinned to the bottom of the screen.
 *  - tap the card            -> open the full-screen player
 *  - list icon               -> "Up next" queue
 *  - swipe left / right      -> next / previous song
 *  - tap or drag the bar     -> seek inside the song
 */
@Composable
fun MiniPlayer(
    song: Song,
    playback: PlaybackState,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onOpen: () -> Unit,
    onQueueClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val duration = if (playback.durationMs > 0) playback.durationMs else song.durationMs

    // Horizontal swipe: the card follows your finger, then springs back
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val swipeThreshold = with(LocalDensity.current) { 80.dp.toPx() }
    val currentOnNext by rememberUpdatedState(onNext)
    val currentOnPrevious by rememberUpdatedState(onPrevious)

    Surface(
        onClick = onOpen,
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()               // stay above the gesture/nav bar
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .graphicsLayer {
                translationX = offsetX.value
                alpha = 1f - (abs(offsetX.value) / (swipeThreshold * 3)).coerceIn(0f, 0.5f)
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        scope.launch {
                            when {
                                offsetX.value <= -swipeThreshold -> currentOnNext()
                                offsetX.value >= swipeThreshold -> currentOnPrevious()
                            }
                            offsetX.animateTo(0f)
                        }
                    },
                    onDragCancel = { scope.launch { offsetX.animateTo(0f) } },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch { offsetX.snapTo(offsetX.value + dragAmount) }
                    }
                )
            },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SongThumbnail(songId = song.id, size = 52.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                // (Previous = swipe right, to leave room for the queue button)
                FilledIconButton(
                    onClick = onPlayPause,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary, // Strawberry Red
                        contentColor = MaterialTheme.colorScheme.onTertiary
                    )
                ) {
                    Icon(
                        painter = painterResource(
                            if (playback.isPlaying) R.drawable.baseline_pause_24 else R.drawable.baseline_play_arrow_24
                        ),
                        contentDescription = if (playback.isPlaying) "Pause" else "Play"
                    )
                }
                IconButton(onClick = onNext) {
                    Icon(painterResource(R.drawable.baseline_skip_next_24), contentDescription = "Next")
                }
                IconButton(onClick = onQueueClick) {
                    Icon(painterResource(R.drawable.baseline_queue_music_24), contentDescription = "Up next")
                }
            }

            MiniSeekBar(
                positionMs = playback.positionMs,
                durationMs = duration,
                onSeek = onSeek
            )
        }
    }
}

/**
 * Thin progress bar you can tap or drag to jump inside the song.
 * While dragging it shows where you'll land; the seek is sent when you let go.
 */
@Composable
private fun MiniSeekBar(positionMs: Long, durationMs: Long, onSeek: (Long) -> Unit) {
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val currentDuration by rememberUpdatedState(durationMs)
    val currentOnSeek by rememberUpdatedState(onSeek)

    val playedFraction = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val shownFraction = dragFraction ?: playedFraction
    val shownPosition = dragFraction?.let { (it * durationMs).toLong() } ?: positionMs

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(formatDuration(shownPosition), style = MaterialTheme.typography.labelSmall)
        Box(
            modifier = Modifier
                .weight(1f)
                .height(28.dp) // taller than the bar = easier to hit with a finger
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val f = (offset.x / size.width).coerceIn(0f, 1f)
                        currentOnSeek((f * currentDuration).toLong())
                    }
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { dragFraction = (it.x / size.width).coerceIn(0f, 1f) },
                        onDragEnd = {
                            dragFraction?.let { currentOnSeek((it * currentDuration).toLong()) }
                            dragFraction = null
                        },
                        onDragCancel = { dragFraction = null },
                        onHorizontalDrag = { change, _ ->
                            change.consume() // so the card itself doesn't treat this as a swipe
                            dragFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            LinearProgressIndicator(
                progress = { shownFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (dragFraction != null) 6.dp else 4.dp),
                color = MaterialTheme.colorScheme.tertiary,
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
                strokeCap = StrokeCap.Round
            )
        }
        Text(formatDuration(durationMs), style = MaterialTheme.typography.labelSmall)
    }
}
