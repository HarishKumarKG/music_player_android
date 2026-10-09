package com.harish.mediaplayer.playback

import com.harish.mediaplayer.domain.model.PlaybackState
import com.harish.mediaplayer.domain.model.SavedPlayback
import com.harish.mediaplayer.domain.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Single source of truth shared by the service, the notification and the UI.
 * Also keeps the original (sorted) order so shuffle can be switched off again.
 */
object PlaybackStateHolder {
    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private var originalQueue: List<Song> = emptyList() // the list order the user saw

    internal fun update(transform: (PlaybackState) -> PlaybackState) {
        _state.value = transform(_state.value)
    }

    /** New queue from the song list (in its current sort order), starting at [startIndex]. */
    internal fun setQueue(songs: List<Song>, startIndex: Int) {
        originalQueue = songs
        update {
            if (it.shuffle) {
                // chosen song first, everything else shuffled after it
                val first = songs[startIndex]
                it.copy(queue = listOf(first) + (songs - first).shuffled(), currentIndex = 0)
            } else {
                it.copy(queue = songs, currentIndex = startIndex)
            }
        }
    }

    internal fun setIndex(index: Int) = update { it.copy(currentIndex = index) }

    internal fun toggleShuffle() = update { s ->
        val current = s.currentSong
        if (!s.shuffle) {
            // ON: keep the current song playing, shuffle the rest after it
            val rest = (originalQueue.ifEmpty { s.queue }).filter { it.id != current?.id }.shuffled()
            val newQueue = if (current != null) listOf(current) + rest else rest
            s.copy(shuffle = true, queue = newQueue, currentIndex = if (current != null) 0 else -1)
        } else {
            // OFF: back to the sorted order, still on the same song
            val newQueue = originalQueue.ifEmpty { s.queue }
            s.copy(shuffle = false, queue = newQueue, currentIndex = newQueue.indexOfFirst { it.id == current?.id })
        }
    }

    /** What gets saved to disk so the app can come back to the last song. */
    internal fun snapshot(): SavedPlayback? {
        val s = _state.value
        if (s.currentSong == null) return null
        return SavedPlayback(s.queue, originalQueue.ifEmpty { s.queue }, s.currentIndex, s.positionMs, s.shuffle)
    }

    /** App start: bring back the last queue, paused at the saved position. */
    internal fun restore(saved: SavedPlayback) {
        if (_state.value.currentSong != null) return // something is already playing, keep it
        originalQueue = saved.originalQueue
        val song = saved.queue.getOrNull(saved.currentIndex) ?: return
        _state.value = PlaybackState(
            queue = saved.queue,
            currentIndex = saved.currentIndex,
            isPlaying = false,
            positionMs = saved.positionMs,
            durationMs = song.durationMs,
            shuffle = saved.shuffle
        )
    }

    /** Stop: forget the current song (mini player hides) but keep the shuffle preference. */
    internal fun clear() = update { PlaybackState(shuffle = it.shuffle) }
}
