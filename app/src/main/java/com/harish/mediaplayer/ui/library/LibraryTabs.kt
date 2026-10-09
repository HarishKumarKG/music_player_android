package com.harish.mediaplayer.ui.library

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.harish.mediaplayer.R
import com.harish.mediaplayer.domain.library.FolderSort
import com.harish.mediaplayer.domain.library.browse
import com.harish.mediaplayer.domain.library.browseParent
import com.harish.mediaplayer.domain.library.groupAlbums
import com.harish.mediaplayer.domain.library.groupFolders
import com.harish.mediaplayer.domain.library.sortedByFolderSort
import com.harish.mediaplayer.domain.library.storageLabel
import com.harish.mediaplayer.domain.library.storageRootOf
import com.harish.mediaplayer.domain.model.LibraryData
import com.harish.mediaplayer.domain.model.Playlist
import com.harish.mediaplayer.domain.model.Song
import com.harish.mediaplayer.ui.common.SongThumbnail
import com.harish.mediaplayer.ui.common.songCount
import java.io.File

/** The four tabs under the header. */
enum class LibraryTab(val label: String, @DrawableRes val icon: Int) {
    SONGS("Songs", R.drawable.baseline_music_note_24),
    PLAYLISTS("Playlists", R.drawable.baseline_queue_music_24),
    FOLDERS("Folders", R.drawable.baseline_folder_24),
    BROWSE("Browse", R.drawable.baseline_smartphone_24)
}

@Composable
fun SongsTab(
    songs: List<Song>,
    isLoading: Boolean,
    handlers: SongListHandlers,
    contentPadding: PaddingValues
) {
    if (songs.isEmpty()) {
        EmptyState(if (isLoading) null else "No songs found on this device")
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = contentPadding) {
        items(songs, key = { it.id }, contentType = { "song" }) { song ->
            SongRow(song = song, list = songs, handlers = handlers)
            ListDivider()
        }
    }
}

@Composable
fun PlaylistsTab(
    songs: List<Song>,
    library: LibraryData,
    onOpen: (SongCollection) -> Unit,
    onNewPlaylist: () -> Unit,
    onRename: (Playlist) -> Unit,
    onDelete: (Playlist) -> Unit,
    contentPadding: PaddingValues
) {
    val albums = remember(songs) { songs.groupAlbums() }
    val favoriteCount = remember(songs, library.favorites) { songs.count { it.id in library.favorites } }

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = contentPadding) {
        item {
            CollectionRow(
                title = "Favourites",
                subtitle = songCount(favoriteCount),
                onClick = { onOpen(SongCollection.Favourites) },
                leading = { IconTile(R.drawable.baseline_favorite_24, gradient = true) }
            )
        }

        item {
            SectionHeader("My playlists") {
                TextButton(onClick = onNewPlaylist) {
                    Icon(painterResource(R.drawable.baseline_add_24), contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("New")
                }
            }
        }
        if (library.playlists.isEmpty()) {
            item {
                Text(
                    "No playlists yet. Tap New, or use ⋮ → Add to playlist on any song.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
        items(library.playlists, key = { "pl" + it.id }) { p ->
            CollectionRow(
                title = p.name,
                subtitle = songCount(p.songIds.size),
                onClick = { onOpen(SongCollection.CustomPlaylist(p.id)) },
                leading = { IconTile(R.drawable.baseline_queue_music_24) },
                menu = listOf(
                    MenuEntry("Rename") { onRename(p) },
                    MenuEntry("Delete") { onDelete(p) }
                )
            )
        }

        item { SectionHeader("Albums") }
        items(albums, key = { "al" + it.name }) { album ->
            CollectionRow(
                title = album.name,
                subtitle = "${album.artist} · ${songCount(album.songs.size)}",
                onClick = { onOpen(SongCollection.Album(album.name)) },
                leading = { SongThumbnail(songId = album.songs.first().id) } // album art from its first song
            )
        }
    }
}

@Composable
fun FoldersTab(
    songs: List<Song>,
    sort: FolderSort,
    onOpen: (SongCollection) -> Unit,
    contentPadding: PaddingValues
) {
    val folders = remember(songs, sort) { songs.groupFolders().sortedByFolderSort(sort) }
    if (folders.isEmpty()) {
        EmptyState("No folders with songs")
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = contentPadding) {
        items(folders, key = { it.path }) { folder ->
            CollectionRow(
                title = folder.name,
                subtitle = "${songCount(folder.songs.size)} · ${folder.path}",
                onClick = { onOpen(SongCollection.Folder(folder.path)) },
                leading = { IconTile(R.drawable.baseline_folder_24) },
                trailingIcon = R.drawable.baseline_chevron_right_24
            )
            ListDivider()
        }
    }
}

@Composable
fun BrowseTab(songs: List<Song>, handlers: SongListHandlers, contentPadding: PaddingValues) {
    var dir by rememberSaveable { mutableStateOf<String?>(null) } // null = list of storages
    val listing = remember(songs, dir) { songs.browse(dir) }

    // Back goes one folder up while browsing
    BackHandler(enabled = dir != null) { dir = browseParent(dir!!) }

    Column(modifier = Modifier.fillMaxSize()) {
        Breadcrumb(dir = dir, onUp = { dir = dir?.let(::browseParent) })
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = contentPadding) {
            items(listing.dirs, key = { "d" + it.path }) { d ->
                CollectionRow(
                    title = d.label,
                    subtitle = songCount(d.songCount),
                    onClick = { dir = d.path },
                    leading = {
                        IconTile(
                            when {
                                !d.isStorageRoot -> R.drawable.baseline_folder_24
                                d.path.startsWith("/storage/emulated") -> R.drawable.baseline_smartphone_24
                                else -> R.drawable.baseline_sd_card_24
                            }
                        )
                    },
                    trailingIcon = R.drawable.baseline_chevron_right_24
                )
            }
            items(listing.songs, key = { "s" + it.id }, contentType = { "song" }) { song ->
                // queue = the songs in this folder, in file-name order
                SongRow(song = song, list = listing.songs, handlers = handlers, subtitle = File(song.path).name)
            }
        }
    }
}

/** "Internal storage › Music › Tamil" with an up arrow. */
@Composable
private fun Breadcrumb(dir: String?, onUp: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onUp, enabled = dir != null) {
            Icon(painterResource(R.drawable.baseline_arrow_back_24), contentDescription = "Up one folder")
        }
        val text = if (dir == null) "Storage" else {
            val root = storageRootOf("$dir/x")
            (listOf(storageLabel(root)) + dir.removePrefix(root).split('/').filter { it.isNotEmpty() })
                .joinToString("  ›  ")
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState(Int.MAX_VALUE)) // long paths: show the end
        )
    }
}

@Composable
fun CollectionScreen(
    title: String,
    songs: List<Song>,
    handlers: SongListHandlers,
    onBack: () -> Unit,
    onPlayAll: () -> Unit,
    contentPadding: PaddingValues,
    headerMenu: List<MenuEntry> = emptyList(),
    extraSongMenu: (Song) -> List<MenuEntry> = { emptyList() },
    emptyText: String = "No songs here yet",
    /** Non-null = custom playlist: show drag handles and save the new order. */
    onReorder: ((List<Song>) -> Unit)? = null
) {
    BackHandler(onBack = onBack)
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(painterResource(R.drawable.baseline_arrow_back_24), contentDescription = "Back")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(songCount(songs.size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (songs.isNotEmpty()) {
                FilledTonalButton(onClick = onPlayAll) {
                    Icon(painterResource(R.drawable.baseline_play_arrow_24), contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Play all")
                }
            }
            if (headerMenu.isNotEmpty()) OverflowMenu(headerMenu)
        }
        if (songs.isEmpty()) {
            EmptyState(emptyText)
        } else if (onReorder != null) {
            ReorderableSongList(songs, handlers, extraSongMenu, contentPadding, onReorder)
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = contentPadding) {
                items(songs, key = { it.id }, contentType = { "song" }) { song ->
                    SongRow(song = song, list = songs, handlers = handlers, extraMenu = extraSongMenu(song))
                    ListDivider()
                }
            }
        }
    }
}

/**
 * Playlist songs you can reorder by dragging the ≡ handle.
 * While dragging, the row follows your finger; when it passes half a row it swaps
 * with its neighbour (which slides out of the way). Letting go saves the new order.
 */
@Composable
private fun ReorderableSongList(
    songs: List<Song>,
    handlers: SongListHandlers,
    extraSongMenu: (Song) -> List<MenuEntry>,
    contentPadding: PaddingValues,
    onReorder: (List<Song>) -> Unit
) {
    // Local copy we can move items around in; reset whenever the saved playlist changes
    val items = remember(songs) { songs.toMutableStateList() }
    var draggingId by remember { mutableStateOf<Long?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var rowHeight by remember { mutableFloatStateOf(0f) }
    val currentOnReorder by rememberUpdatedState(onReorder)

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = contentPadding) {
        items(items, key = { it.id }, contentType = { "song" }) { song ->
            val isDragging = song.id == draggingId
            Column(
                modifier = if (isDragging) {
                    Modifier
                        .zIndex(1f)
                        .graphicsLayer {
                            translationY = dragOffset
                            shadowElevation = 16f
                        }
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                } else {
                    Modifier.animateItem() // neighbours slide smoothly when they swap
                }
            ) {
                SongRow(
                    song = song,
                    list = items,
                    handlers = handlers,
                    extraMenu = extraSongMenu(song),
                    modifier = Modifier.onSizeChanged { rowHeight = it.height.toFloat() },
                    dragHandle = {
                        Icon(
                            painter = painterResource(R.drawable.baseline_drag_handle_24),
                            contentDescription = "Drag to reorder",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(start = 4.dp, end = 12.dp)
                                .pointerInput(song.id) {
                                    detectDragGestures(
                                        onDragStart = {
                                            draggingId = song.id
                                            dragOffset = 0f
                                        },
                                        onDragEnd = {
                                            draggingId = null
                                            dragOffset = 0f
                                            currentOnReorder(items.toList()) // save
                                        },
                                        onDragCancel = {
                                            draggingId = null
                                            dragOffset = 0f
                                        },
                                        onDrag = { change, amount ->
                                            change.consume()
                                            dragOffset += amount.y
                                            val i = items.indexOfFirst { it.id == song.id }
                                            if (rowHeight > 0f && i >= 0) {
                                                if (dragOffset > rowHeight / 2 && i < items.lastIndex) {
                                                    items.add(i + 1, items.removeAt(i)) // swap with the one below
                                                    dragOffset -= rowHeight
                                                } else if (dragOffset < -rowHeight / 2 && i > 0) {
                                                    items.add(i - 1, items.removeAt(i)) // swap with the one above
                                                    dragOffset += rowHeight
                                                }
                                            }
                                        }
                                    )
                                }
                        )
                    }
                )
                ListDivider()
            }
        }
    }
}

/** Centered message, or a spinner when [text] is null. */
@Composable
private fun EmptyState(text: String?) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (text == null) CircularProgressIndicator()
        else Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(32.dp))
    }
}
