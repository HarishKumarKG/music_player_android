package com.harish.mediaplayer.domain.model

/** A user-made playlist. Songs are stored by MediaStore id, in the order they were added. */
data class Playlist(val id: Long, val name: String, val songIds: List<Long>)

/** Everything the user curates by hand. */
data class LibraryData(
    val favorites: Set<Long> = emptySet(),
    val playlists: List<Playlist> = emptyList()
)
