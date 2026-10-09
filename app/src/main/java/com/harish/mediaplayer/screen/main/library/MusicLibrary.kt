package com.harish.mediaplayer.screen.main.library

import com.harish.mediaplayer.screen.main.model.Song
import java.io.File

/** Pure helpers that turn the flat song list into albums, folders and a browsable tree. */

data class AlbumGroup(val name: String, val artist: String, val songs: List<Song>)
data class FolderGroup(val path: String, val name: String, val songs: List<Song>)

private val byNameIgnoringCase = String.CASE_INSENSITIVE_ORDER

fun List<Song>.groupAlbums(): List<AlbumGroup> =
    groupBy { it.album }
        .map { (album, songs) ->
            val artist = songs.map { it.artist }.distinct().singleOrNull() ?: "Various artists"
            AlbumGroup(album, artist, songs)
        }
        .sortedWith(compareBy(byNameIgnoringCase) { it.name })

/** Every folder that directly contains songs. */
fun List<Song>.groupFolders(): List<FolderGroup> =
    groupBy { songParent(it) }
        .map { (path, songs) -> FolderGroup(path, path.substringAfterLast('/').ifEmpty { path }, songs) }
        .sortedWith(compareBy(byNameIgnoringCase) { it.name })

/** How the Folders tab is ordered. */
enum class FolderSort(val label: String) {
    NAME_ASC("Name (A–Z)"),
    NAME_DESC("Name (Z–A)"),
    MOST_SONGS("Most songs"),
    RECENTLY_UPDATED("Recently updated") // folder with the newest song first
}

fun List<FolderGroup>.sortedByFolderSort(sort: FolderSort): List<FolderGroup> = when (sort) {
    FolderSort.NAME_ASC -> sortedWith(compareBy(byNameIgnoringCase) { it.name })
    FolderSort.NAME_DESC -> sortedWith(compareByDescending(byNameIgnoringCase) { it.name })
    FolderSort.MOST_SONGS -> sortedByDescending { it.songs.size }
    FolderSort.RECENTLY_UPDATED -> sortedByDescending { f -> f.songs.maxOf { it.dateModifiedSec } }
}

fun songParent(song: Song): String = File(song.path).parent ?: "/"

// ------------------------------------------------------------------ Browse (directory tree)

data class BrowseDir(val path: String, val label: String, val songCount: Int, val isStorageRoot: Boolean)
data class BrowseListing(val dirs: List<BrowseDir>, val songs: List<Song>)

/** "/storage/emulated/0/Music/a.mp3" -> "/storage/emulated/0"; "/storage/1A2B-3C4D/x.mp3" -> "/storage/1A2B-3C4D" */
fun storageRootOf(path: String): String {
    val parts = path.split('/').filter { it.isNotEmpty() }
    return when {
        parts.size >= 3 && parts[0] == "storage" && parts[1] == "emulated" -> "/storage/emulated/${parts[2]}"
        parts.size >= 2 -> "/${parts[0]}/${parts[1]}"
        else -> "/"
    }
}

fun storageLabel(root: String): String = when {
    root == "/storage/emulated/0" -> "Internal storage"
    root.startsWith("/storage/emulated/") -> "Internal storage (user ${root.substringAfterLast('/')})"
    root.startsWith("/storage/") -> "SD card (${root.substringAfterLast('/')})"
    else -> root
}

/**
 * What's inside [dir]: sub-folders that (somewhere below) contain songs, plus the songs right here.
 * dir == null is the top level: one entry per storage (internal, SD card...).
 */
fun List<Song>.browse(dir: String?): BrowseListing {
    if (dir == null) {
        val roots = groupBy { storageRootOf(it.path) }
            .map { (root, songs) -> BrowseDir(root, storageLabel(root), songs.size, isStorageRoot = true) }
            .sortedBy { it.label }
        return BrowseListing(roots, emptyList())
    }
    val prefix = dir.trimEnd('/') + "/"
    val (here, deeper) = filter { it.path.startsWith(prefix) }
        .partition { '/' !in it.path.removePrefix(prefix) }
    val dirs = deeper
        .groupBy { prefix + it.path.removePrefix(prefix).substringBefore('/') }
        .map { (path, songs) -> BrowseDir(path, path.substringAfterLast('/'), songs.size, isStorageRoot = false) }
        .sortedWith(compareBy(byNameIgnoringCase) { it.label })
    return BrowseListing(dirs, here.sortedWith(compareBy(byNameIgnoringCase) { File(it.path).name }))
}

/** Parent folder in the browser; a storage root goes back to the top level (null). */
fun browseParent(dir: String): String? =
    if (dir == storageRootOf("$dir/x")) null else dir.substringBeforeLast('/')

// ------------------------------------------------------------------ Opened collections

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
