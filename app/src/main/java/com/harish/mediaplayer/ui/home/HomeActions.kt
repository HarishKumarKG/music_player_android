package com.harish.mediaplayer.ui.home

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
