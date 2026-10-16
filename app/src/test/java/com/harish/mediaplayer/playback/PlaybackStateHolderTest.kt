package com.harish.mediaplayer.playback

import com.harish.mediaplayer.song
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PlaybackStateHolderTest {

    private val list = (1L..10L).map { song(it) }

    @Before
    fun setUp() {
        PlaybackStateHolder.clear()
        if (PlaybackStateHolder.state.value.shuffle) PlaybackStateHolder.toggleShuffle()
    }

    @After
    fun tearDown() = PlaybackStateHolder.clear()

    @Test
    fun `playing from a list keeps the list order and starts at the tapped song`() {
        PlaybackStateHolder.setQueue(list, startIndex = 3)
        val state = PlaybackStateHolder.state.value
        assertEquals(list, state.queue)
        assertEquals(4L, state.currentSong?.id)
    }

    @Test
    fun `shuffle on keeps the current song first, shuffle off restores list order`() {
        PlaybackStateHolder.setQueue(list, startIndex = 5)

        PlaybackStateHolder.toggleShuffle()
        val shuffled = PlaybackStateHolder.state.value
        assertTrue(shuffled.shuffle)
        assertEquals(6L, shuffled.currentSong?.id)
        assertEquals(0, shuffled.currentIndex)
        assertEquals(list.toSet(), shuffled.queue.toSet())

        PlaybackStateHolder.toggleShuffle()
        val restored = PlaybackStateHolder.state.value
        assertFalse(restored.shuffle)
        assertEquals(list, restored.queue)
        assertEquals(6L, restored.currentSong?.id)
    }

    @Test
    fun `snapshot and restore bring back the same song and position`() {
        PlaybackStateHolder.setQueue(list, startIndex = 2)
        PlaybackStateHolder.update { it.copy(positionMs = 42_000) }
        val saved = PlaybackStateHolder.snapshot()!!

        PlaybackStateHolder.clear()
        PlaybackStateHolder.restore(saved)

        val state = PlaybackStateHolder.state.value
        assertEquals(3L, state.currentSong?.id)
        assertEquals(42_000L, state.positionMs)
        assertFalse(state.isPlaying)
    }

    @Test
    fun `shuffle play uses every song once, turns shuffle on, and off goes back to list order`() {
        PlaybackStateHolder.setQueueShuffled(list)
        val shuffled = PlaybackStateHolder.state.value
        assertTrue(shuffled.shuffle)
        assertEquals(0, shuffled.currentIndex)
        assertEquals(list.map { it.id }.toSet(), shuffled.queue.map { it.id }.toSet())
        assertEquals(list.size, shuffled.queue.size)

        val playing = shuffled.currentSong
        PlaybackStateHolder.toggleShuffle()
        val sorted = PlaybackStateHolder.state.value
        assertFalse(sorted.shuffle)
        assertEquals(list, sorted.queue)
        assertEquals(playing?.id, sorted.currentSong?.id) // same song keeps playing
    }

    @Test
    fun `renamed song gets its new path in the queue and stays current`() {
        PlaybackStateHolder.setQueue(list, startIndex = 2)
        val renamed = list[2].copy(path = "/storage/emulated/0/Music/New name.mp3")

        PlaybackStateHolder.replaceSongs(listOf(renamed))

        val state = PlaybackStateHolder.state.value
        assertEquals(renamed, state.currentSong)
        assertEquals(list.size, state.queue.size)
        assertEquals(list[3], state.queue[3]) // others untouched
    }
}
