package com.harish.mediaplayer.domain.library

import com.harish.mediaplayer.domain.model.Song
import java.io.File

data class AlbumGroup(val name: String, val artist: String, val songs: List<Song>)

private val byNameIgnoringCase = String.CASE_INSENSITIVE_ORDER

fun List<Song>.groupAlbums(): List<AlbumGroup> =
    groupBy { it.album }
        .map { (album, songs) ->
            val artist = songs.map { it.artist }.distinct().singleOrNull() ?: "Various artists"
            AlbumGroup(album, artist, songs)
        }
        .sortedWith(compareBy(byNameIgnoringCase) { it.name })

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

/**
 * Every song in [dir] AND all its sub-folders, folder by folder, in file-name order.
 * Example: "Music" with a.mp3, Tamil/b.mp3, Tamil/Old/c.mp3 -> [a, b, c].
 * Used by Shuffle play / Play inside the Browse tab.
 */
fun List<Song>.songsUnder(dir: String): List<Song> {
    val prefix = dir.trimEnd('/') + "/"
    return filter { it.path.startsWith(prefix) }.sortedWith(
        compareBy<Song, String>(byNameIgnoringCase) { File(it.path).parent.orEmpty() }
            .thenBy(byNameIgnoringCase) { File(it.path).name }
    )
}

/** Parent folder in the browser; a storage root goes back to the top level (null). */
fun browseParent(dir: String): String? =
    if (dir == storageRootOf("$dir/x")) null else dir.substringBeforeLast('/')
