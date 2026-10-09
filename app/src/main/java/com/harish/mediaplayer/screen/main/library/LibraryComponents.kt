package com.harish.mediaplayer.screen.main.library

import android.text.format.Formatter
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.harish.mediaplayer.R
import com.harish.mediaplayer.library.Playlist
import com.harish.mediaplayer.screen.main.SongThumbnail
import com.harish.mediaplayer.screen.main.formatDuration
import com.harish.mediaplayer.screen.main.model.Song
import com.harish.mediaplayer.ui.brand.themedBrandGradient
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

/** One entry in a ⋮ menu. */
data class MenuEntry(val label: String, @DrawableRes val icon: Int? = null, val onClick: () -> Unit)

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

/** ⋮ button with a dropdown. */
@Composable
fun OverflowMenu(entries: List<MenuEntry>) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(painterResource(R.drawable.baseline_more_vert_24), contentDescription = "More options")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            entries.forEach { entry ->
                DropdownMenuItem(
                    text = { Text(entry.label) },
                    leadingIcon = entry.icon?.let { { Icon(painterResource(it), contentDescription = null) } },
                    onClick = {
                        expanded = false
                        entry.onClick()
                    }
                )
            }
        }
    }
}

/** Row for an album / playlist / folder: leading art, title, subtitle, optional ⋮ menu. */
@Composable
fun CollectionRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    leading: @Composable () -> Unit,
    menu: List<MenuEntry> = emptyList(),
    trailingIcon: Int? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leading()
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (menu.isNotEmpty()) OverflowMenu(menu)
        trailingIcon?.let {
            Icon(painterResource(it), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 12.dp))
        }
    }
}

/** Rounded square with an icon, used as artwork for favourites / playlists / folders. */
@Composable
fun IconTile(@DrawableRes icon: Int, gradient: Boolean = false) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (gradient) Modifier.background(Brush.linearGradient(themedBrandGradient()))
                else Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painterResource(icon), contentDescription = null,
            tint = if (gradient) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SectionHeader(text: String, action: (@Composable () -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        action?.invoke()
    }
}

@Composable
fun ListDivider() = HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

fun songCount(n: Int) = if (n == 1) "1 song" else "$n songs"

// ------------------------------------------------------------------ dialogs

/** Pick a playlist for [song], or start a new one. */
@Composable
fun AddToPlaylistDialog(
    playlists: List<Playlist>,
    onPick: (Playlist) -> Unit,
    onNewPlaylist: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to playlist") },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                item {
                    CollectionRow(
                        title = "New playlist",
                        subtitle = "Create and add this song",
                        onClick = onNewPlaylist,
                        leading = { IconTile(R.drawable.baseline_add_24, gradient = true) }
                    )
                }
                items(playlists, key = { it.id }) { p ->
                    CollectionRow(
                        title = p.name,
                        subtitle = songCount(p.songIds.size),
                        onClick = { onPick(p) },
                        leading = { IconTile(R.drawable.baseline_queue_music_24) }
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/** Text-field dialog used for "New playlist" and "Rename". */
@Composable
fun NameDialog(
    title: String,
    initial: String = "",
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text("Playlist name") }
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ConfirmDialog(title: String, message: String, confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmLabel, color = MaterialTheme.colorScheme.tertiary) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun SongInfoDialog(song: Song, onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(song.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoRow("Artist", song.artist)
                InfoRow("Album", song.album)
                InfoRow("Duration", formatDuration(song.durationMs))
                InfoRow("Size", Formatter.formatShortFileSize(context, song.sizeBytes))
                InfoRow("Format", song.mimeType)
                InfoRow(
                    "Date modified",
                    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                        .format(Date(song.dateModifiedSec * 1000))
                )
                InfoRow("Path", song.path)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
