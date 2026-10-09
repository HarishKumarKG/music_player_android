package com.harish.mediaplayer.data.song

import com.harish.mediaplayer.domain.model.Song
import org.json.JSONArray
import org.json.JSONObject

/** Song <-> JSON, shared by the song-list cache and the "last played" save file. */
fun Song.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("title", title)
    .put("artist", artist)
    .put("album", album)
    .put("durationMs", durationMs)
    .put("sizeBytes", sizeBytes)
    .put("mimeType", mimeType)
    .put("dateModifiedSec", dateModifiedSec)
    .put("path", path)

fun JSONObject.toSong(): Song = Song(
    id = getLong("id"),
    title = getString("title"),
    artist = getString("artist"),
    album = getString("album"),
    durationMs = getLong("durationMs"),
    sizeBytes = getLong("sizeBytes"),
    mimeType = getString("mimeType"),
    dateModifiedSec = getLong("dateModifiedSec"),
    path = getString("path")
)

fun List<Song>.toJsonArray(): JSONArray = JSONArray().also { array -> forEach { array.put(it.toJson()) } }

fun JSONArray.toSongs(): List<Song> = List(length()) { i -> getJSONObject(i).toSong() }
