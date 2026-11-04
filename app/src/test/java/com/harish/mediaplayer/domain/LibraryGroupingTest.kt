package com.harish.mediaplayer.domain

import com.harish.mediaplayer.domain.library.browse
import com.harish.mediaplayer.domain.library.browseParent
import com.harish.mediaplayer.domain.library.groupAlbums
import com.harish.mediaplayer.domain.library.songsUnder
import com.harish.mediaplayer.domain.library.storageLabel
import com.harish.mediaplayer.domain.library.storageRootOf
import com.harish.mediaplayer.song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LibraryGroupingTest {

    private val internal = "/storage/emulated/0"
    private val songs = listOf(
        song(1, path = "$internal/Music/Tamil/a.mp3", album = "Hits", artist = "A"),
        song(2, path = "$internal/Music/Tamil/b.mp3", album = "Hits", artist = "B"),
        song(3, path = "$internal/Music/c.mp3", album = "Solo", artist = "C"),
        song(4, path = "/storage/1A2B-3C4D/Songs/d.mp3", album = "Solo", artist = "C")
    )

    @Test
    fun `albums group by name and mixed artists become Various artists`() {
        val albums = songs.groupAlbums()
        assertEquals(listOf("Hits", "Solo"), albums.map { it.name })
        assertEquals("Various artists", albums.first { it.name == "Hits" }.artist)
        assertEquals("C", albums.first { it.name == "Solo" }.artist)
    }

    @Test
    fun `storage roots and labels`() {
        assertEquals(internal, storageRootOf("$internal/Music/x.mp3"))
        assertEquals("/storage/1A2B-3C4D", storageRootOf("/storage/1A2B-3C4D/Songs/d.mp3"))
        assertEquals("Internal storage", storageLabel(internal))
        assertEquals("SD card (1A2B-3C4D)", storageLabel("/storage/1A2B-3C4D"))
    }

    @Test
    fun `browse top level lists one entry per storage`() {
        val top = songs.browse(null)
        assertEquals(setOf("Internal storage", "SD card (1A2B-3C4D)"), top.dirs.map { it.label }.toSet())
        assertEquals(emptyList<Any>(), top.songs)
    }

    @Test
    fun `browse a folder shows sub folders and the songs directly inside`() {
        val music = songs.browse("$internal/Music")
        assertEquals(listOf("Tamil"), music.dirs.map { it.label })
        assertEquals(2, music.dirs.single().songCount)
        assertEquals(listOf(3L), music.songs.map { it.id })
    }

    @Test
    fun `going up from a storage root returns to the top level`() {
        assertEquals("$internal/Music", browseParent("$internal/Music/Tamil"))
        assertEquals(internal, browseParent("$internal/Music"))
        assertNull(browseParent(internal))
    }

    @Test
    fun `songs under a folder include every sub folder, folder by folder`() {
        val m = "/storage/emulated/0/Music"
        val list = listOf(
            song(1, path = "$m/Tamil/Old/c.mp3"),
            song(2, path = "$m/b.mp3"),
            song(3, path = "$m/Tamil/a.mp3"),
            song(4, path = "$m/a.mp3"),
            song(5, path = "/storage/emulated/0/Musical/x.mp3"), // different folder, similar name
            song(6, path = "/storage/emulated/0/Podcasts/p.mp3")
        )
        assertEquals(listOf(4L, 2L, 3L, 1L), list.songsUnder(m).map { it.id })
        assertEquals(listOf(3L, 1L), list.songsUnder("$m/Tamil/").map { it.id })
    }
}
