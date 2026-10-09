package com.harish.mediaplayer.screen.main

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.harish.mediaplayer.R
import com.harish.mediaplayer.library.LibraryData
import com.harish.mediaplayer.library.Playlist
import com.harish.mediaplayer.screen.main.library.AddToPlaylistDialog
import com.harish.mediaplayer.screen.main.library.BrowseTab
import com.harish.mediaplayer.screen.main.library.CollectionScreen
import com.harish.mediaplayer.screen.main.library.ConfirmDialog
import com.harish.mediaplayer.screen.main.library.FolderSort
import com.harish.mediaplayer.screen.main.library.FoldersTab
import com.harish.mediaplayer.screen.main.library.LibraryTab
import com.harish.mediaplayer.screen.main.library.MenuEntry
import com.harish.mediaplayer.screen.main.library.NameDialog
import com.harish.mediaplayer.screen.main.library.PlaylistsTab
import com.harish.mediaplayer.screen.main.library.SongCollection
import com.harish.mediaplayer.screen.main.library.SongInfoDialog
import com.harish.mediaplayer.screen.main.library.SongListHandlers
import com.harish.mediaplayer.screen.main.library.SongsTab
import com.harish.mediaplayer.screen.main.library.songParent
import com.harish.mediaplayer.screen.main.model.Song
import com.harish.mediaplayer.screen.main.model.SortOrder
import com.harish.mediaplayer.screen.main.model.sortedByOrder
import com.harish.mediaplayer.ui.brand.themedBrandGradient
import com.harish.mediaplayer.ui.brand.GradientTitle
import com.harish.mediaplayer.ui.brand.HarishLogo
import com.harish.mediaplayer.ui.theme.BrandBackground
import com.harish.mediaplayer.ui.theme.LocalIsDarkTheme
import com.harish.mediaplayer.screen.theme.ThemeViewModel
import com.harish.mediaplayer.settings.ThemeMode
import com.harish.mediaplayer.util.PlaybackState
import com.harish.mediaplayer.util.PlaybackStateHolder
import com.harish.mediaplayer.util.PlayerController
import kotlinx.coroutines.delay

/** Favourite / playlist operations, bundled for the UI. */
class LibraryActions(
    val toggleFavorite: (songId: Long) -> Unit,
    val createPlaylist: (name: String, firstSongId: Long?) -> Unit,
    val renamePlaylist: (id: Long, name: String) -> Unit,
    val deletePlaylist: (id: Long) -> Unit,
    val addToPlaylist: (id: Long, songId: Long) -> Unit,
    val removeFromPlaylist: (id: Long, songId: Long) -> Unit,
    val reorderPlaylist: (id: Long, orderedIds: List<Long>) -> Unit
)

@Composable
fun MainScreenContent(
    viewModel: MainScreenViewModel = hiltViewModel(),
    context: Context,
    onMenuClick: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val library by viewModel.library.collectAsStateWithLifecycle()
    // Comes from the service, so the list, mini player and notification always agree
    val playback by PlaybackStateHolder.state.collectAsStateWithLifecycle()
    // Same ViewModel as the Theme screen, so the header toggle and the Theme screen always agree
    val themeViewModel: ThemeViewModel = hiltViewModel()
    val isDark = LocalIsDarkTheme.current

    // One-off messages after a manual refresh ("2 new songs found", ...)
    LaunchedEffect(Unit) {
        viewModel.messages.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }

    MainScreen(
        state = state,
        library = library,
        playback = playback,
        onRefresh = { viewModel.onEvent(OnRefreshEvent) },
        onMenuClick = onMenuClick,
        isDark = isDark,
        // One tap flips Light <-> Dark (if it was "System default", it flips away from the current look)
        onToggleTheme = { themeViewModel.onThemeSelected(if (isDark) ThemeMode.LIGHT else ThemeMode.DARK) },
        actions = PlayerActions(
            playFromList = { list, index -> PlayerController.playFromList(context, list, index) },
            playQueueItem = { index -> PlayerController.playQueueItem(context, index) },
            togglePlayPause = { PlayerController.togglePlayPause(context) },
            next = { PlayerController.next(context) },
            previous = { PlayerController.previous(context) },
            seek = { positionMs -> PlayerController.seekTo(context, positionMs) },
            toggleShuffle = { PlayerController.toggleShuffle(context) }
        ),
        libraryActions = LibraryActions(
            toggleFavorite = viewModel::toggleFavorite,
            createPlaylist = viewModel::createPlaylist,
            renamePlaylist = viewModel::renamePlaylist,
            deletePlaylist = viewModel::deletePlaylist,
            addToPlaylist = viewModel::addToPlaylist,
            removeFromPlaylist = viewModel::removeFromPlaylist,
            reorderPlaylist = viewModel::reorderPlaylist
        ),
        onMessage = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen(
    state: MainScreenState,
    library: LibraryData,
    playback: PlaybackState,
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
    val currentSong = playback.currentSong

    var selectedTab by rememberSaveable { mutableStateOf(LibraryTab.SONGS) }
    var openKey by rememberSaveable { mutableStateOf<String?>(null) } // opened album/playlist/folder
    var showNowPlaying by rememberSaveable { mutableStateOf(false) }
    var showQueue by rememberSaveable { mutableStateOf(false) }

    // Dialog state
    var infoSong by remember { mutableStateOf<Song?>(null) }
    var addToPlaylistSong by remember { mutableStateOf<Song?>(null) }
    var newPlaylistFor by remember { mutableStateOf<Song?>(null) }   // song to add after creating
    var showNewPlaylist by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<Playlist?>(null) }
    var deleteTarget by remember { mutableStateOf<Playlist?>(null) }

    // If playback is stopped (e.g. from the notification), close the full-screen player
    LaunchedEffect(currentSong == null) { if (currentSong == null) showNowPlaying = false }

    val handlers = SongListHandlers(
        currentSongId = currentSong?.id,
        isPlaying = playback.isPlaying,
        favorites = library.favorites,
        onPlay = { list, song ->
            // current song: pause/resume; otherwise the list you tapped in becomes the queue
            if (song.id == currentSong?.id) actions.togglePlayPause()
            else actions.playFromList(list, list.indexOf(song))
        },
        onToggleFavorite = { libraryActions.toggleFavorite(it.id) },
        onAddToPlaylist = { addToPlaylistSong = it },
        onInfo = { infoSong = it }
    )

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

                SongsHeader(
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
    if (showQueue && playback.queue.isNotEmpty()) {
        QueueSheet(
            playback = playback,
            onSongClick = { index -> actions.playQueueItem(index) },
            onShuffleClick = actions.toggleShuffle,
            onDismiss = { showQueue = false }
        )
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

@Composable
private fun SongsHeader(
    onMenuClick: () -> Unit,
    isDark: Boolean,
    onToggleTheme: () -> Unit,
    isRefreshing: Boolean,
    onRefresh: (() -> Unit)?,  // null = hide refresh
    sort: HeaderSort?          // null = hide sort
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
                .background(Brush.linearGradient(themedBrandGradient()))
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
        ThemeToggleButton(isDark = isDark, onToggle = onToggleTheme)
        onRefresh?.let { RefreshButton(isRefreshing = isRefreshing, onClick = it) }
        sort?.let { SortMenu(it) }
    }
}

/**
 * Sun / moon toggle. Shows what you'll switch TO: a moon in light mode, a sun in dark mode.
 * The icon spins and pops as it swaps.
 */
@Composable
private fun ThemeToggleButton(isDark: Boolean, onToggle: () -> Unit) {
    IconButton(onClick = onToggle) {
        AnimatedContent(
            targetState = isDark,
            transitionSpec = {
                (fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.4f)) togetherWith
                    (fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.4f))
            },
            label = "themeToggle"
        ) { dark ->
            // Each new icon also does a quarter turn while it appears
            val turn = remember { Animatable(-90f) }
            LaunchedEffect(Unit) { turn.animateTo(0f, tween(400, easing = FastOutSlowInEasing)) }
            Icon(
                painter = painterResource(if (dark) R.drawable.baseline_light_mode_24 else R.drawable.baseline_dark_mode_24),
                contentDescription = if (dark) "Switch to light mode" else "Switch to dark mode",
                modifier = Modifier.graphicsLayer { rotationZ = turn.value }
            )
        }
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

/** Sort choices shown in the header for whatever list is on screen. */
class HeaderSort(val options: List<String>, val selected: Int, val onSelect: (Int) -> Unit)

private fun songHeaderSort(current: SortOrder, onSelect: (SortOrder) -> Unit) = HeaderSort(
    options = SortOrder.entries.map { it.label },
    selected = current.ordinal,
    onSelect = { onSelect(SortOrder.entries[it]) }
)

@Composable
private fun SortMenu(sort: HeaderSort) {
    var expanded by remember { mutableStateOf(false) }
    // Box anchors the dropdown to the icon so it opens from the top-right
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                painter = painterResource(id = R.drawable.baseline_sort_24),
                contentDescription = "Sort"
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            sort.options.forEachIndexed { index, label ->
                val isSelected = index == sort.selected
                DropdownMenuItem(
                    text = {
                        Text(
                            text = label,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.tertiary
                                    else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        sort.onSelect(index)
                        expanded = false
                    }
                )
            }
        }
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
