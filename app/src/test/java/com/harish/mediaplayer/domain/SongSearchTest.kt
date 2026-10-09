package com.harish.mediaplayer.domain

import com.harish.mediaplayer.domain.library.search
import com.harish.mediaplayer.song
import org.junit.Assert.assertEquals
import org.junit.Test

class SongSearchTest {

    private val songs = listOf(
        song(1, title = "Ilamai Itho Itho", artist = "Ilaiyaraaja", album = "80s Hits"),
        song(2, title = "Café del Mar", artist = "Energy 52", album = "Chill"),
        song(3, title = "Track 07", artist = "Unknown", album = "Unknown", path = "/storage/emulated/0/Music/Vaathi Coming.mp3")
    )

    @Test
    fun `matches title ignoring case`() =
        assertEquals(listOf(1L), songs.search("ilamai").map { it.id })

    @Test
    fun `every word must match, in any field and any order`() =
        assertEquals(listOf(1L), songs.search("hits raaja").map { it.id })

    @Test
    fun `accents are ignored`() =
        assertEquals(listOf(2L), songs.search("cafe").map { it.id })

    @Test
    fun `file name is searched too`() =
        assertEquals(listOf(3L), songs.search("vaathi").map { it.id })

    @Test
    fun `blank query returns nothing`() =
        assertEquals(emptyList<Long>(), songs.search("   ").map { it.id })
}
