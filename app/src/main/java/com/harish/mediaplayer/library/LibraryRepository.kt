package com.harish.mediaplayer.library

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** A user-made playlist. Songs are stored by MediaStore id, in the order they were added. */
data class Playlist(val id: Long, val name: String, val songIds: List<Long>)

/** Everything the user curates by hand. */
data class LibraryData(
    val favorites: Set<Long> = emptySet(),
    val playlists: List<Playlist> = emptyList()
)

/**
 * Favourites + playlists, saved to `library.json` in the app's private storage.
 * Every change updates the StateFlow instantly (UI reacts) and is written to disk in the background.
 */
@Singleton
class LibraryRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val file = File(context.filesDir, "library.json")

    @OptIn(ExperimentalCoroutinesApi::class)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))

    private val _data = MutableStateFlow(LibraryData())
    val data: StateFlow<LibraryData> = _data.asStateFlow()

    init {
        scope.launch { load() }
    }

    fun toggleFavorite(songId: Long) = change { d ->
        d.copy(favorites = if (songId in d.favorites) d.favorites - songId else d.favorites + songId)
    }

    /** New playlist, optionally starting with one song (from "Add to playlist > New playlist"). */
    fun createPlaylist(name: String, firstSongId: Long? = null) = change { d ->
        d.copy(playlists = d.playlists + Playlist(System.currentTimeMillis(), name.trim(), listOfNotNull(firstSongId)))
    }

    fun renamePlaylist(id: Long, name: String) = change { d ->
        d.copy(playlists = d.playlists.map { if (it.id == id) it.copy(name = name.trim()) else it })
    }

    fun deletePlaylist(id: Long) = change { d -> d.copy(playlists = d.playlists.filterNot { it.id == id }) }

    fun addToPlaylist(id: Long, songId: Long) = change { d ->
        d.copy(playlists = d.playlists.map {
            if (it.id == id && songId !in it.songIds) it.copy(songIds = it.songIds + songId) else it
        })
    }

    fun removeFromPlaylist(id: Long, songId: Long) = change { d ->
        d.copy(playlists = d.playlists.map { if (it.id == id) it.copy(songIds = it.songIds - songId) else it })
    }

    /**
     * New order after drag & drop. [orderedIds] are the songs the user could see; ids of songs
     * that no longer exist on the phone are kept at the end so nothing is silently lost.
     */
    fun reorderPlaylist(id: Long, orderedIds: List<Long>) = change { d ->
        d.copy(playlists = d.playlists.map { p ->
            if (p.id != id) p else p.copy(songIds = orderedIds + p.songIds.filterNot { it in orderedIds })
        })
    }

    private fun change(transform: (LibraryData) -> LibraryData) {
        _data.update(transform)
        val snapshot = _data.value
        scope.launch { save(snapshot) }
    }

    private fun load() {
        if (!file.exists()) return
        try {
            val json = JSONObject(file.readText())
            val favs = json.getJSONArray("favorites").let { a -> List(a.length()) { a.getLong(it) }.toSet() }
            val lists = json.getJSONArray("playlists").let { a ->
                List(a.length()) { i ->
                    val p = a.getJSONObject(i)
                    val ids = p.getJSONArray("songIds").let { s -> List(s.length()) { s.getLong(it) } }
                    Playlist(p.getLong("id"), p.getString("name"), ids)
                }
            }
            _data.value = LibraryData(favs, lists)
        } catch (_: Exception) {
            // unreadable file: keep empty data rather than crash
        }
    }

    private fun save(data: LibraryData) {
        try {
            val json = JSONObject()
                .put("favorites", JSONArray(data.favorites.toList()))
                .put("playlists", JSONArray().also { arr ->
                    data.playlists.forEach { p ->
                        arr.put(
                            JSONObject().put("id", p.id).put("name", p.name).put("songIds", JSONArray(p.songIds))
                        )
                    }
                })
            val tmp = File(file.parentFile, file.name + ".tmp")
            tmp.writeText(json.toString())
            tmp.renameTo(file)
        } catch (_: Exception) {
            // best-effort
        }
    }
}
