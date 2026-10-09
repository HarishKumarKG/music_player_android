package com.harish.mediaplayer.ui.player

import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.harish.mediaplayer.R
import com.harish.mediaplayer.domain.model.PlaybackState
import com.harish.mediaplayer.domain.model.Song
import com.harish.mediaplayer.ui.brand.BrandRed
import com.harish.mediaplayer.ui.common.SongArtwork
import com.harish.mediaplayer.ui.common.formatDuration
import com.harish.mediaplayer.ui.theme.BrandBackground

/**
 * Full-screen player (opens from the mini player):
 * big artwork, title/artist, seek slider, and large controls.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NowPlayingScreen(
    song: Song,
    playback: PlaybackState,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onShuffleClick: () -> Unit,
    onQueueClick: () -> Unit,
    onClose: () -> Unit
) {
    val duration = if (playback.durationMs > 0) playback.durationMs else song.durationMs

    // Artwork "breathes": full size while playing, shrinks a little when paused
    val artScale by animateFloatAsState(
        targetValue = if (playback.isPlaying) 1f else 0.86f,
        animationSpec = spring(dampingRatio = 0.6f),
        label = "artScale"
    )

    BrandBackground(modifier = Modifier.fillMaxSize()) {
        // Blurred copy of the artwork tinting the background (blur needs Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            SongArtwork(
                songId = song.id,
                sizeHint = 120.dp,
                cornerRadius = 0.dp,
                showPlaceholder = false,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(80.dp)
                    .graphicsLayer { alpha = 0.45f }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar: collapse button + "NOW PLAYING"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        painterResource(R.drawable.baseline_expand_more_24),
                        contentDescription = "Close player",
                        modifier = Modifier.size(32.dp)
                    )
                }
                Text(
                    text = "NOW PLAYING",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 3.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(48.dp)) // balances the close button
            }

            Spacer(modifier = Modifier.weight(1f))

            // Big artwork with a soft shadow
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .graphicsLayer { scaleX = artScale; scaleY = artScale }
                    .shadow(elevation = 24.dp, shape = RoundedCornerShape(28.dp))
            ) {
                SongArtwork(
                    songId = song.id,
                    sizeHint = 360.dp,
                    cornerRadius = 28.dp,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Column(modifier = Modifier.fillMaxWidth()) {
                // Title scrolls sideways if it's too long (marquee)
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .basicMarquee()
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Seek slider: shows the drag position while dragging, seeks on release
                var dragFraction by remember { mutableStateOf<Float?>(null) }
                val playedFraction =
                    if (duration > 0) (playback.positionMs.toFloat() / duration).coerceIn(0f, 1f) else 0f
                Slider(
                    value = dragFraction ?: playedFraction,
                    onValueChange = { dragFraction = it },
                    onValueChangeFinished = {
                        dragFraction?.let { onSeek((it * duration).toLong()) }
                        dragFraction = null
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.tertiary,
                        activeTrackColor = MaterialTheme.colorScheme.tertiary,
                        inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth()) {
                    val shownPosition = dragFraction?.let { (it * duration).toLong() } ?: playback.positionMs
                    Text(formatDuration(shownPosition), style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(formatDuration(duration), style = MaterialTheme.typography.labelMedium)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Big controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Shuffle: red when on
                    IconButton(onClick = onShuffleClick) {
                        Icon(
                            painterResource(R.drawable.baseline_shuffle_24),
                            contentDescription = if (playback.shuffle) "Shuffle on" else "Shuffle off",
                            tint = if (playback.shuffle) MaterialTheme.colorScheme.tertiary
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onPrevious, modifier = Modifier.size(64.dp)) {
                        Icon(
                            painterResource(R.drawable.baseline_skip_previous_24),
                            contentDescription = "Previous",
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    // Strawberry Red play/pause button with a red glow
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .shadow(16.dp, CircleShape, ambientColor = BrandRed, spotColor = BrandRed)
                            .clip(CircleShape)
                            .background(BrandRed),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(onClick = onPlayPause, modifier = Modifier.size(80.dp)) {
                            Icon(
                                painter = painterResource(
                                    if (playback.isPlaying) R.drawable.baseline_pause_24 else R.drawable.baseline_play_arrow_24
                                ),
                                contentDescription = if (playback.isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }
                    IconButton(onClick = onNext, modifier = Modifier.size(64.dp)) {
                        Icon(
                            painterResource(R.drawable.baseline_skip_next_24),
                            contentDescription = "Next",
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    // Up next list
                    IconButton(onClick = onQueueClick) {
                        Icon(
                            painterResource(R.drawable.baseline_queue_music_24),
                            contentDescription = "Up next",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
