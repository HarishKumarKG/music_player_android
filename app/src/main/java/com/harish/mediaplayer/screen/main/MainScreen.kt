package com.harish.mediaplayer.screen.main

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import android.text.format.Formatter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import com.harish.mediaplayer.ui.brand.BrandGradient
import com.harish.mediaplayer.ui.brand.GradientTitle
import androidx.compose.ui.unit.sp
import com.harish.mediaplayer.ui.brand.HarishLogo
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.graphics.Color
import com.harish.mediaplayer.ui.theme.BrandBackground
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.harish.mediaplayer.R
import com.harish.mediaplayer.screen.main.model.Song
import com.harish.mediaplayer.screen.main.model.SortOrder
import com.harish.mediaplayer.screen.main.model.sortedByOrder
import java.text.DateFormat
import java.util.Date
import com.harish.mediaplayer.util.PlayerController
import com.harish.mediaplayer.util.PlaybackState
import com.harish.mediaplayer.util.PlaybackStateHolder

@Composable
fun MainScreenContent(
    viewModel: MainScreenViewModel = hiltViewModel(),
    context: Context,
    onMenuClick: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Comes from the service, so the list, mini player and notification always agree
    val playback by PlaybackStateHolder.state.collectAsStateWithLifecycle()

    // One-off messages after a manual refresh ("2 new songs found", ...)
    LaunchedEffect(Unit) {
        viewModel.messages.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }

    MainScreen(
        state = state,
        playback = playback,
        onRefresh = { viewModel.onEvent(OnRefreshEvent) },
        onMenuClick = onMenuClick,
        actions = PlayerActions(
            playFromList = { list, index -> PlayerController.playFromList(context, list, index) },
            playQueueItem = { index -> PlayerController.playQueueItem(context, index) },
            togglePlayPause = { PlayerController.togglePlayPause(context) },
            next = { PlayerController.next(context) },
            previous = { PlayerController.previous(context) },
            seek = { positionMs -> PlayerController.seekTo(context, positionMs) },
            toggleShuffle = { PlayerController.toggleShuffle(context) }
        )
    )
}

@Composable
private fun MainScreen(
    state: MainScreenState,
    playback: PlaybackState,
    onRefresh: () -> Unit,
    onMenuClick: () -> Unit,
    actions: PlayerActions
) {
    val songs = state.songs
    var infoSong by remember { mutableStateOf<Song?>(null) }
    var showNowPlaying by rememberSaveable { mutableStateOf(false) }

    // System Back closes the full-screen player first
    BackHandler(enabled = showNowPlaying) { showNowPlaying = false }
    var sortOrder by rememberSaveable { mutableStateOf(SortOrder.DATE_MODIFIED) } // survives rotation
    val sortedSongs = remember(songs, sortOrder) { songs.sortedByOrder(sortOrder) }
    // The song comes from the play queue, so it stays correct even after a refresh/sort
    val currentSong = playback.currentSong
    var showQueue by rememberSaveable { mutableStateOf(false) }

    // If playback is stopped (e.g. from the notification), close the full-screen player
    LaunchedEffect(currentSong == null) { if (currentSong == null) showNowPlaying = false }

    /** Tapping a song in the list: pause/resume if it's the current one, else start the queue there. */
    fun onSongTapped(song: Song) {
        if (song.id == currentSong?.id) actions.togglePlayPause()
        else actions.playFromList(sortedSongs, sortedSongs.indexOf(song)) // queue = list in current sort order
    }

    BrandBackground(modifier = Modifier.fillMaxSize()) {
        // No top bar: Scaffold still gives us the system-bar insets in innerPadding,
        // so the header sits just below the status bar (edge-to-edge safe).
        Scaffold(
            containerColor = Color.Transparent, // let the colourful background show through
            bottomBar = {
                // Slides up the first time a song starts
                AnimatedVisibility(
                    visible = currentSong != null,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut()
                ) {
                    currentSong?.let { song ->
                        MiniPlayer(
                            song = song,
                            playback = playback,
                            onPlayPause = actions.togglePlayPause,
                            onPrevious = actions.previous,
                            onNext = actions.next,
                            onSeek = actions.seek,
                            onOpen = { showNowPlaying = true },
                            onQueueClick = { showQueue = true }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding())
            ) {
                SongsHeader(
                    onMenuClick = onMenuClick,
                    isRefreshing = state.isRefreshing,
                    onRefresh = onRefresh,
                    sortOrder = sortOrder,
                    onSortSelect = { sortOrder = it }
                )

                if (sortedSongs.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        // Very first launch (no cache yet): show a spinner instead of "no songs"
                        if (state.isLoading) CircularProgressIndicator()
                        else Text("No songs found on this device")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        // list scrolls behind the nav bar, but the last item can still scroll clear of it
                        contentPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding())
                    ) {
                        items(sortedSongs, key = { it.id }) { song ->
                            SongItem(
                                song = song,
                                isCurrent = song.id == currentSong?.id,
                                isPlaying = song.id == currentSong?.id && playback.isPlaying,
                                onPlayPauseClick = { onSongTapped(song) },
                                onInfoClick = { infoSong = song }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }

        // Full-screen player slides up over everything
        AnimatedVisibility(
            visible = showNowPlaying && currentSong != null,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            currentSong?.let { song ->
                NowPlayingScreen(
                    song = song,
                    playback = playback,
                    onPlayPause = actions.togglePlayPause,
                    onPrevious = actions.previous,
                    onNext = actions.next,
                    onSeek = actions.seek,
                    onShuffleClick = actions.toggleShuffle,
                    onQueueClick = { showQueue = true },
                    onClose = { showNowPlaying = false }
                )
            }
        }
    } // BrandBackground

    infoSong?.let { song ->
        SongInfoDialog(song = song, onDismiss = { infoSong = null })
    }

    // "Up next" list, opened from the mini player or the full-screen player
    if (showQueue && playback.queue.isNotEmpty()) {
        QueueSheet(
            playback = playback,
            onSongClick = { index -> actions.playQueueItem(index) },
            onShuffleClick = actions.toggleShuffle,
            onDismiss = { showQueue = false }
        )
    }
}

@Composable
private fun SongsHeader(
    onMenuClick: () -> Unit,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    sortOrder: SortOrder,
    onSortSelect: (SortOrder) -> Unit
) {
    val titleIn = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(500) // let the logo start first
        titleIn.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onMenuClick) {
            Icon(painterResource(R.drawable.baseline_menu_24), contentDescription = "Open menu")
        }
        Spacer(modifier = Modifier.width(4.dp))
        // Gradient badge, same look as the app icon
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(BrandGradient))
        ) {
            HarishLogo(
                loop = false,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { scaleX = 1.3f; scaleY = 1.3f } // zoom so the H fills the badge
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .graphicsLayer {
                    alpha = titleIn.value
                    translationX = (1f - titleIn.value) * -16.dp.toPx() // slide in from the logo
                }
        ) {
            GradientTitle(text = "Harish")
            Text(
                text = "MUSIC PLAYER",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 3.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        RefreshButton(isRefreshing = isRefreshing, onClick = onRefresh)
        SortMenu(selected = sortOrder, onSelect = onSortSelect)
    }
}

@Composable
private fun RefreshButton(isRefreshing: Boolean, onClick: () -> Unit) {
    // Swap the icon for a small spinner while MediaStore is being re-scanned
    IconButton(onClick = onClick, enabled = !isRefreshing) {
        if (isRefreshing) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            Icon(
                painter = painterResource(id = R.drawable.baseline_refresh_24),
                contentDescription = "Refresh songs"
            )
        }
    }
}

@Composable
private fun SortMenu(selected: SortOrder, onSelect: (SortOrder) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    // Box anchors the dropdown to the icon so it opens from the top-right
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                painter = painterResource(id = R.drawable.baseline_sort_24),
                contentDescription = "Sort songs"
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SortOrder.entries.forEach { order ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = order.label,
                            fontWeight = if (order == selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (order == selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        onSelect(order)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun SongItem(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onPlayPauseClick: () -> Unit,
    onInfoClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlayPauseClick)
            .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SongThumbnail(songId = song.id)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = song.title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isCurrent) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onPlayPauseClick) {
            Icon(
                painter = painterResource(
                    id = if (isPlaying) R.drawable.baseline_pause_24 else R.drawable.baseline_play_arrow_24
                ),
                contentDescription = if (isPlaying) "Pause" else "Play"
            )
        }
        IconButton(onClick = onInfoClick) {
            Icon(
                painter = painterResource(id = R.drawable.baseline_info_24),
                contentDescription = "Song info"
            )
        }
    }
}

@Composable
private fun SongInfoDialog(song: Song, onDismiss: () -> Unit) {
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
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

internal fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%d:%02d".format(minutes, seconds)
}
