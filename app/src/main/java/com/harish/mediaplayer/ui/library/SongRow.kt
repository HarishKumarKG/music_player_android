package com.harish.mediaplayer.ui.library

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.harish.mediaplayer.R
import com.harish.mediaplayer.domain.model.Song
import com.harish.mediaplayer.ui.common.SongThumbnail
import kotlinx.coroutines.launch

/** What every song row needs to know / do, shared by all tabs. */
class SongListHandlers(
    val currentSongId: Long?,
    val isPlaying: Boolean,
    val favorites: Set<Long>,
    /** Play [song] with [list] as the queue (or pause/resume if it's already the current song). */
    val onPlay: (list: List<Song>, song: Song) -> Unit,
    val onToggleFavorite: (Song) -> Unit,
    val onAddToPlaylist: (Song) -> Unit,
    val onInfo: (Song) -> Unit
)

/**
 * Song row: artwork (with a play/pause badge on the current song), title + artist,
 * a heart for favourites and a ⋮ menu (Add to playlist, Song info, + extras).
 */
@Composable
fun SongRow(
    song: Song,
    list: List<Song>,
    handlers: SongListHandlers,
    extraMenu: List<MenuEntry> = emptyList(),
    subtitle: String = song.artist,
    modifier: Modifier = Modifier,
    dragHandle: (@Composable () -> Unit)? = null
) {
    val isCurrent = song.id == handlers.currentSongId
    val isFavorite = song.id in handlers.favorites

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { handlers.onPlay(list, song) }
            .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(contentAlignment = Alignment.Center) {
            SongThumbnail(songId = song.id)
            if (isCurrent) {
                // Dark badge over the artwork shows what tapping will do
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painterResource(if (handlers.isPlaying) R.drawable.baseline_pause_24 else R.drawable.baseline_play_arrow_24),
                        contentDescription = if (handlers.isPlaying) "Pause" else "Play",
                        tint = Color.White
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isCurrent) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        FavoriteButton(isFavorite = isFavorite, onClick = { handlers.onToggleFavorite(song) })
        OverflowMenu(
            entries = listOf(
                MenuEntry("Add to playlist", R.drawable.baseline_playlist_add_24) { handlers.onAddToPlaylist(song) }
            ) + extraMenu + MenuEntry("Song info", R.drawable.baseline_info_24) { handlers.onInfo(song) }
        )
        dragHandle?.invoke()
    }
}

/** Heart that "pops" (springy scale) when you favourite a song. */
@Composable
fun FavoriteButton(isFavorite: Boolean, onClick: () -> Unit) {
    val scope = rememberCoroutineScope()
    val scale = remember { Animatable(1f) }
    IconButton(onClick = {
        onClick()
        scope.launch {
            scale.snapTo(0.6f)
            scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioHighBouncy))
        }
    }) {
        Icon(
            painter = painterResource(if (isFavorite) R.drawable.baseline_favorite_24 else R.drawable.baseline_favorite_border_24),
            contentDescription = if (isFavorite) "Remove from favourites" else "Add to favourites",
            tint = if (isFavorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.graphicsLayer { scaleX = scale.value; scaleY = scale.value }
        )
    }
}
