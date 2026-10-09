package com.harish.mediaplayer.screen.main.repository

import android.content.Context
import android.provider.MediaStore
import com.harish.mediaplayer.screen.main.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import com.harish.mediaplayer.screen.main.model.toJsonArray
import com.harish.mediaplayer.screen.main.model.toSongs
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Songs come from two places:
 *  1. A small JSON cache in the app's private storage -> instant list on app start.
 *  2. MediaStore (the real source)                    -> used to refresh the cache.
 */
@Singleton
class SongRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val cacheFile = File(context.filesDir, "songs_cache.json")

    /** Last saved list, or empty if the app has never loaded songs before. */
    suspend fun loadCached(): List<Song> = withContext(Dispatchers.IO) {
        if (!cacheFile.exists()) return@withContext emptyList()
        try {
            JSONArray(cacheFile.readText()).toSongs()
        } catch (e: Exception) {
            emptyList() // corrupt/old cache: ignore it, a refresh will rebuild it
        }
    }

    /** Reads MediaStore and saves the result as the new cache. */
    suspend fun refresh(): List<Song> = withContext(Dispatchers.IO) {
        val songs = fetchSongs(context)
        saveCache(songs)
        songs
    }

    private fun saveCache(songs: List<Song>) {
        val array = songs.toJsonArray()
        // Write to a temp file then rename, so a crash mid-write can't leave a broken cache
        val tmp = File(cacheFile.parentFile, cacheFile.name + ".tmp")
        tmp.writeText(array.toString())
        tmp.renameTo(cacheFile)
    }
}

private fun fetchSongs(context: Context): List<Song> {
    val songList = mutableListOf<Song>()
    val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.SIZE,
        MediaStore.Audio.Media.MIME_TYPE,
        MediaStore.Audio.Media.DATE_MODIFIED,
        MediaStore.Audio.Media.DATA // file path, used by MediaPlayerService
    )
    val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
    val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

    context.contentResolver.query(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, null, sortOrder
    )?.use { c ->
        val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
        val durationCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
        val sizeCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
        val mimeCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
        val dateModCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
        val dataCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

        while (c.moveToNext()) {
            songList.add(
                Song(
                    id = c.getLong(idCol),
                    title = c.getString(titleCol) ?: "Unknown title",
                    artist = c.getString(artistCol).orUnknown(),
                    album = c.getString(albumCol).orUnknown(),
                    durationMs = c.getLong(durationCol),
                    sizeBytes = c.getLong(sizeCol),
                    mimeType = c.getString(mimeCol) ?: "Unknown",
                    dateModifiedSec = c.getLong(dateModCol),
                    path = c.getString(dataCol) ?: ""
                )
            )
        }
    }
    return songList
}

// MediaStore returns "<unknown>" when a tag is missing
private fun String?.orUnknown(): String =
    if (this.isNullOrBlank() || this == "<unknown>") "Unknown" else this
