package com.harish.mediaplayer.ui.library

/** Something you can open to see its songs. [key] is a plain string so it survives rotation. */
sealed class SongCollection(val key: String) {
    data object Favourites : SongCollection("fav")
    data class Album(val name: String) : SongCollection("album:$name")
    data class CustomPlaylist(val id: Long) : SongCollection("pl:$id")
    data class Folder(val path: String) : SongCollection("folder:$path")

    companion object {
        fun fromKey(key: String): SongCollection? = when {
            key == "fav" -> Favourites
            key.startsWith("album:") -> Album(key.removePrefix("album:"))
            key.startsWith("pl:") -> key.removePrefix("pl:").toLongOrNull()?.let { CustomPlaylist(it) }
            key.startsWith("folder:") -> Folder(key.removePrefix("folder:"))
            else -> null
        }
    }
}
