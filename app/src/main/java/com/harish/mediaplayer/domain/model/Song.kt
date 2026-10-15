package com.harish.mediaplayer.domain.model

/** One audio file read from MediaStore. */
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val mimeType: String,
    val dateModifiedSec: Long, // seconds since epoch (MediaStore.DATE_MODIFIED)
    val path: String
) {
    /** "Kannalane.mp3" — the real file name (the list shows [title], from the song's tags). */
    val fileName: String get() = path.substringAfterLast('/')
}

/** File names of the other songs in the same folder as [song], so a rename can't clash with one. */
fun List<Song>.otherNamesInFolderOf(song: Song): List<String> {
    val folder = song.path.substringBeforeLast('/')
    return filter { it.id != song.id && it.path.substringBeforeLast('/') == folder }.map { it.fileName }
}

/** Sort options shown in the top-right menu. */
enum class SortOrder(val label: String) {
    DATE_MODIFIED("Date modified"),
    NAME_ASC("Name (A–Z)"),
    NAME_DESC("Name (Z–A)")
}

fun List<Song>.sortedByOrder(order: SortOrder): List<Song> = when (order) {
    SortOrder.DATE_MODIFIED -> sortedByDescending { it.dateModifiedSec } // newest first
    SortOrder.NAME_ASC -> sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
    SortOrder.NAME_DESC -> sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.title })
}
