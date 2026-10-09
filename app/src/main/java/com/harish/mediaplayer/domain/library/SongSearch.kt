package com.harish.mediaplayer.domain.library

import com.harish.mediaplayer.domain.model.Song
import java.io.File
import java.text.Normalizer
import java.util.Locale

/**
 * Pre-built search data for a song list.
 *
 * Building it normalizes every song's text ONCE (lower case, accents removed); after that each
 * search is just fast `contains` checks. Build it off the main thread for big libraries.
 *
 * Matching rules:
 * - looks in title, artist, album and file name
 * - ignores upper/lower case and accents ("cafe" finds "Café")
 * - every word must match somewhere, in any order: "raja hits" finds a song by Ilaiyaraaja
 *   on the album "80s Hits"
 */
class SongSearchIndex(songs: List<Song>) {

    private val entries: List<Pair<Song, String>> = songs.map { song ->
        song to normalize("${song.title} ${song.artist} ${song.album} ${File(song.path).nameWithoutExtension}")
    }

    /** Songs matching [query], in the original list order. */
    fun search(query: String): List<Song> {
        val words = normalize(query).split(' ').filter { it.isNotBlank() }
        if (words.isEmpty()) return emptyList()
        return entries.mapNotNull { (song, text) -> song.takeIf { words.all { it in text } } }
    }

    private companion object {
        private val accents = Regex("\\p{Mn}+")

        fun normalize(text: String): String =
            Normalizer.normalize(text, Normalizer.Form.NFD)
                .replace(accents, "") // strip accents
                .lowercase(Locale.ROOT)
    }
}

/** One-off search (builds a throw-away index). Prefer keeping a [SongSearchIndex] for repeated searches. */
fun List<Song>.search(query: String): List<Song> = SongSearchIndex(this).search(query)
