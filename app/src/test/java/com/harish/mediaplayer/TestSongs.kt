package com.harish.mediaplayer

import com.harish.mediaplayer.domain.model.Song

/** Small helper so tests can create songs in one line. */
fun song(
    id: Long,
    title: String = "Song $id",
    path: String = "/storage/emulated/0/Music/$title.mp3",
    album: String = "Album",
    artist: String = "Artist",
    dateModifiedSec: Long = id
) = Song(
    id = id,
    title = title,
    artist = artist,
    album = album,
    durationMs = 180_000,
    sizeBytes = 4_000_000,
    mimeType = "audio/mpeg",
    dateModifiedSec = dateModifiedSec,
    path = path
)
