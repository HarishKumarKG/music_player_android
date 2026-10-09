package com.harish.mediaplayer.ui.home

import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.harish.mediaplayer.R
import com.harish.mediaplayer.domain.library.FolderSort
import com.harish.mediaplayer.domain.library.songParent
import com.harish.mediaplayer.domain.model.LibraryData
import com.harish.mediaplayer.domain.model.PlaybackState
import com.harish.mediaplayer.domain.model.Playlist
import com.harish.mediaplayer.domain.model.Song
import com.harish.mediaplayer.domain.model.SortOrder
import com.harish.mediaplayer.domain.model.ThemeMode
import com.harish.mediaplayer.domain.model.sortedByOrder
import com.harish.mediaplayer.playback.PlaybackStateHolder
import com.harish.mediaplayer.playback.PlayerController
import com.harish.mediaplayer.ui.library.AddToPlaylistDialog
import com.harish.mediaplayer.ui.library.BrowseTab
import com.harish.mediaplayer.ui.library.CollectionScreen
import com.harish.mediaplayer.ui.library.ConfirmDialog
import com.harish.mediaplayer.ui.library.FoldersTab
import com.harish.mediaplayer.ui.library.LibraryTab
import com.harish.mediaplayer.ui.library.MenuEntry
import com.harish.mediaplayer.ui.library.NameDialog
import com.harish.mediaplayer.ui.library.PlaylistsTab
import com.harish.mediaplayer.ui.library.SongCollection
import com.harish.mediaplayer.ui.library.SongInfoDialog
import com.harish.mediaplayer.ui.library.SongListHandlers
import com.harish.mediaplayer.ui.library.SongsTab
import com.harish.mediaplayer.ui.player.MiniPlayer
import com.harish.mediaplayer.ui.player.NowPlayingScreen
import com.harish.mediaplayer.ui.player.PlayerActions
import com.harish.mediaplayer.ui.player.QueueSheet
import com.harish.mediaplayer.ui.player.SleepTimerSheetHost
import com.harish.mediaplayer.ui.theme.BrandBackground
import com.harish.mediaplayer.ui.theme.LocalIsDarkTheme
import com.harish.mediaplayer.ui.themepicker.ThemeViewModel

@Composable
fun HomeRoute(
    onMenuClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val library by viewModel.library.collectAsStateWithLifecycle()
    // Comes from the service, so the list, mini player and notification always agree.
    // Kept as a State (not read here!) so the 2x-per-second position updates only redraw
    // the player UI that shows the position, not this whole screen and its song list.
    val playbackState = PlaybackStateHolder.state.collectAsStateWithLifecycle()
    // Same ViewModel as the Theme screen, so the header toggle and the Theme screen always agree
    val themeViewModel: ThemeViewModel = hiltViewModel()
    val isDark = LocalIsDarkTheme.current

    // One-off messages after a manual refresh ("2 new songs found", ...)
    LaunchedEffect(Unit) {
        viewModel.messages.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }

    HomeScreen(
        state = state,
        library = library,
        playbackState = playbackState,
        onRefresh = viewModel::refresh,
        onMenuClick = onMenuClick,
        isDark = isDark,
        // One tap flips Light <-> Dark (if it was "System default", it flips away from the current look)
        onToggleTheme = { themeViewModel.onThemeSelected(if (isDark) ThemeMode.LIGHT else ThemeMode.DARK) },
        // remember(): the same objects every time, so children can skip redrawing
        actions = remember(context) {
            PlayerActions(
                playFromList = { list, index -> PlayerController.playFromList(context, list, index) },
                playQueueItem = { index -> PlayerController.playQueueItem(context, index) },
                togglePlayPause = { PlayerController.togglePlayPause(context) },
                next = { PlayerController.next(context) },
                previous = { PlayerController.previous(context) },
                seek = { positionMs -> PlayerController.seekTo(context, positionMs) },
                toggleShuffle = { PlayerController.toggleShuffle(context) }
            )
        },
        libraryActions = remember(viewModel) {
            LibraryActions(
                toggleFavorite = viewModel::toggleFavorite,
                createPlaylist = viewModel::createPlaylist,
                renamePlaylist = viewModel::renamePlaylist,
                deletePlaylist = viewModel::deletePlaylist,
                addToPlaylist = viewModel::addToPlaylist,
                removeFromPlaylist = viewModel::removeFromPlaylist,
                reorderPlaylist = viewModel::reorderPlaylist
            )
        },
        onMessage = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    state: HomeUiState,
    library: LibraryData,
    playbackState: State<PlaybackState>,
    onRefresh: () -> Unit,
    onMenuClick: () -> Unit,
    isDark: Boolean,
    onToggleTheme: () -> Unit,
    actions: PlayerActions,
    libraryActions: LibraryActions,
    onMessage: (String) -> Unit
) {
    val songs = state.songs
    var sortOrder by rememberSaveable { mutableStateOf(SortOrder.DATE_MODIFIED) } // songs; survives rotation
    var folderSort by rememberSaveable { mutableStateOf(FolderSort.NAME_ASC) }   // Folders tab list
    val sortedSongs = remember(songs, sortOrder) { songs.sortedByOrder(sortOrder) }
    // Only these two coarse values drive the list; derivedStateOf ignores position ticks
    val currentSong by remember { derivedStateOf { playbackState.value.currentSong } }
    val isPlaying by remember { derivedStateOf { playbackState.value.isPlaying } }

    var selectedTab by rememberSaveable { mutableStateOf(LibraryTab.SONGS) }
    var openKey by rememberSaveable { mutableStateOf<String?>(null) } // opened album/playlist/folder
    var showNowPlaying by rememberSaveable { mutableStateOf(false) }
    var showQueue by rememberSaveable { mutableStateOf(false) }
    var showSleepTimer by rememberSaveable { mutableStateOf(false) }

    // Dialog state
    var infoSong by remember { mutableStateOf<Song?>(null) }
    var addToPlaylistSong by remember { mutableStateOf<Song?>(null) }
    var newPlaylistFor by remember { mutableStateOf<Song?>(null) }   // song to add after creating
    var showNewPlaylist by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<Playlist?>(null) }
    var deleteTarget by remember { mutableStateOf<Playlist?>(null) }

    // If playback is stopped (e.g. from the notification), close the full-screen player
    LaunchedEffect(currentSong == null) { if (currentSong == null) showNowPlaying = false }

    val currentSongId = currentSong?.id
    // Rebuilt only when the playing song, play/pause or favourites change -> rows can skip redrawing
    val handlers = remember(currentSongId, isPlaying, library.favorites, actions, libraryActions) {
        SongListHandlers(
            currentSongId = currentSongId,
            isPlaying = isPlaying,
            favorites = library.favorites,
            onPlay = { list, song ->
                // current song: pause/resume; otherwise the list you tapped in becomes the queue
                if (song.id == currentSongId) actions.togglePlayPause()
                else actions.playFromList(list, list.indexOf(song))
            },
            onToggleFavorite = { libraryActions.toggleFavorite(it.id) },
            onAddToPlaylist = { addToPlaylistSong = it },
            onInfo = { infoSong = it }
        )
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
                            playback = playbackState.value, // read here: only the mini player redraws on ticks
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
            val listPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding())
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding())
            ) {
                // Header buttons depend on what's on screen:
                //  songs lists -> song sort + refresh, folder list -> folder sort + refresh,
                //  playlists overview / a custom playlist -> nothing (order is yours), browse -> refresh
                val openedNow = openKey?.let(SongCollection::fromKey)
                val headerSort: HeaderSort? = when {
                    openedNow is SongCollection.CustomPlaylist -> null
                    openedNow != null -> songHeaderSort(sortOrder) { sortOrder = it }
                    selectedTab == LibraryTab.SONGS -> songHeaderSort(sortOrder) { sortOrder = it }
                    selectedTab == LibraryTab.FOLDERS -> HeaderSort(
                        options = FolderSort.entries.map { it.label },
                        selected = folderSort.ordinal,
                        onSelect = { folderSort = FolderSort.entries[it] }
                    )
                    else -> null
                }
                val showRefresh = !(selectedTab == LibraryTab.PLAYLISTS && (openedNow == null || openedNow is SongCollection.CustomPlaylist))

                Column {
                    HomeHeader(
                        onMenuClick = onMenuClick,
                        isDark = isDark,
                        onToggleTheme = onToggleTheme,
                        isRefreshing = state.isRefreshing,
                        onRefresh = onRefresh.takeIf { showRefresh },
                        sort = headerSort
                    )

                    PrimaryTabRow(
                        selectedTabIndex = selectedTab.ordinal,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        LibraryTab.entries.forEach { tab ->
                            Tab(
                                selected = tab == selectedTab,
                                onClick = {
                                    selectedTab = tab
                                    openKey = null // switching tabs closes an opened playlist/folder
                                },
                                text = { Text(tab.label, maxLines = 1) },
                                icon = { Icon(painterResource(tab.icon), contentDescription = null) },
                                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    val opened = openKey?.let(SongCollection::fromKey)
                    if (opened != null) {
                        OpenedCollection(
                            collection = opened,
                            sortedSongs = sortedSongs,
                            library = library,
                            handlers = handlers,
                            libraryActions = libraryActions,
                            playAll = { list -> if (list.isNotEmpty()) actions.playFromList(list, 0) },
                            onRename = { renameTarget = it },
                            onDelete = { deleteTarget = it },
                            onClose = { openKey = null },
                            contentPadding = listPadding
                        )
                    } else when (selectedTab) {
                        LibraryTab.SONGS -> SongsTab(sortedSongs, state.isLoading, handlers, listPadding)
                        LibraryTab.PLAYLISTS -> PlaylistsTab(
                            songs = songs,
                            library = library,
                            onOpen = { openKey = it.key },
                            onNewPlaylist = { showNewPlaylist = true },
                            onRename = { renameTarget = it },
                            onDelete = { deleteTarget = it },
                            contentPadding = listPadding
                        )
                        LibraryTab.FOLDERS -> FoldersTab(songs, folderSort, onOpen = { openKey = it.key }, contentPadding = listPadding)
                        LibraryTab.BROWSE -> BrowseTab(songs, handlers, listPadding)
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
            // Declared after the tabs' BackHandlers, so Back closes the player first
            BackHandler { showNowPlaying = false }
            currentSong?.let { song ->
                NowPlayingScreen(
                    song = song,
                    playback = playbackState.value,
                    onPlayPause = actions.togglePlayPause,
                    onPrevious = actions.previous,
                    onNext = actions.next,
                    onSeek = actions.seek,
                    onShuffleClick = actions.toggleShuffle,
                    onQueueClick = { showQueue = true },
                    onSleepTimerClick = { showSleepTimer = true },
                    onClose = { showNowPlaying = false }
                )
            }
        }
    } // BrandBackground

    // ---------------------------------------------------------------- dialogs & sheets

    infoSong?.let { song -> SongInfoDialog(song = song, onDismiss = { infoSong = null }) }

    addToPlaylistSong?.let { song ->
        AddToPlaylistDialog(
            playlists = library.playlists,
            onPick = { p ->
                libraryActions.addToPlaylist(p.id, song.id)
                onMessage("Added to ${p.name}")
                addToPlaylistSong = null
            },
            onNewPlaylist = {
                newPlaylistFor = song
                addToPlaylistSong = null
            },
            onDismiss = { addToPlaylistSong = null }
        )
    }

    if (showNewPlaylist || newPlaylistFor != null) {
        NameDialog(
            title = "New playlist",
            confirmLabel = "Create",
            onConfirm = { name ->
                libraryActions.createPlaylist(name, newPlaylistFor?.id)
                onMessage(if (newPlaylistFor != null) "Added to $name" else "Playlist created")
                showNewPlaylist = false
                newPlaylistFor = null
            },
            onDismiss = {
                showNewPlaylist = false
                newPlaylistFor = null
            }
        )
    }

    renameTarget?.let { p ->
        NameDialog(
            title = "Rename playlist",
            initial = p.name,
            confirmLabel = "Rename",
            onConfirm = { name ->
                libraryActions.renamePlaylist(p.id, name)
                renameTarget = null
            },
            onDismiss = { renameTarget = null }
        )
    }

    deleteTarget?.let { p ->
        ConfirmDialog(
            title = "Delete \"${p.name}\"?",
            message = "The playlist is removed. Your songs stay on the phone.",
            confirmLabel = "Delete",
            onConfirm = {
                libraryActions.deletePlaylist(p.id)
                if (openKey == SongCollection.CustomPlaylist(p.id).key) openKey = null
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null }
        )
    }

    // "Up next" list, opened from the mini player or the full-screen player
    if (showSleepTimer) {
        SleepTimerSheetHost(onDismiss = { showSleepTimer = false })
    }

    if (showQueue) {
        QueueSheetHost(
            playbackState = playbackState,
            onSongClick = actions.playQueueItem,
            onShuffleClick = actions.toggleShuffle,
            onDismiss = { showQueue = false }
        )
    }
}

/** Reads the live playback state in its own scope, so position ticks only redraw the sheet. */
@Composable
private fun QueueSheetHost(
    playbackState: State<PlaybackState>,
    onSongClick: (Int) -> Unit,
    onShuffleClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val playback = playbackState.value
    if (playback.queue.isNotEmpty()) {
        QueueSheet(playback = playback, onSongClick = onSongClick, onShuffleClick = onShuffleClick, onDismiss = onDismiss)
    }
}

/** Shows the songs of an opened favourites / album / playlist / folder. */
@Composable
private fun OpenedCollection(
    collection: SongCollection,
    sortedSongs: List<Song>,
    library: LibraryData,
    handlers: SongListHandlers,
    libraryActions: LibraryActions,
    playAll: (List<Song>) -> Unit,
    onRename: (Playlist) -> Unit,
    onDelete: (Playlist) -> Unit,
    onClose: () -> Unit,
    contentPadding: PaddingValues
) {
    when (collection) {
        SongCollection.Favourites -> {
            val list = sortedSongs.filter { it.id in library.favorites }
            CollectionScreen(
                title = "Favourites", songs = list, handlers = handlers, onBack = onClose,
                onPlayAll = { playAll(list) }, contentPadding = contentPadding,
                emptyText = "Tap the heart on any song to add it here"
            )
        }
        is SongCollection.Album -> {
            val list = sortedSongs.filter { it.album == collection.name }
            CollectionScreen(
                title = collection.name, songs = list, handlers = handlers, onBack = onClose,
                onPlayAll = { playAll(list) }, contentPadding = contentPadding
            )
        }
        is SongCollection.Folder -> {
            val list = sortedSongs.filter { songParent(it) == collection.path }
            CollectionScreen(
                title = collection.path.substringAfterLast('/'), songs = list, handlers = handlers, onBack = onClose,
                onPlayAll = { playAll(list) }, contentPadding = contentPadding
            )
        }
        is SongCollection.CustomPlaylist -> {
            val playlist = library.playlists.firstOrNull { it.id == collection.id }
            if (playlist == null) {
                LaunchedEffect(Unit) { onClose() } // deleted meanwhile
                return
            }
            // Playlists keep the order songs were added in (not the sort order)
            val byId = remember(sortedSongs) { sortedSongs.associateBy { it.id } }
            val list = playlist.songIds.mapNotNull { byId[it] }
            CollectionScreen(
                title = playlist.name, songs = list, handlers = handlers, onBack = onClose,
                onPlayAll = { playAll(list) }, contentPadding = contentPadding,
                headerMenu = listOf(
                    MenuEntry("Rename") { onRename(playlist) },
                    MenuEntry("Delete playlist") { onDelete(playlist) }
                ),
                extraSongMenu = { song ->
                    listOf(MenuEntry("Remove from playlist", R.drawable.baseline_close_24) {
                        libraryActions.removeFromPlaylist(playlist.id, song.id)
                    })
                },
                onReorder = { newOrder -> libraryActions.reorderPlaylist(playlist.id, newOrder.map { it.id }) },
                emptyText = "Use ⋮ → Add to playlist on any song to fill this playlist"
            )
        }
    }
}
