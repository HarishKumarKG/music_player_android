package com.harish.mediaplayer.domain.model

/** Everything about playback the UI needs. Written by MediaPlayerService / PlayerController. */
data class PlaybackState(
    val queue: List<Song> = emptyList(), // songs in PLAY order (shuffled order when shuffle is on)
    val currentIndex: Int = -1,          // position in [queue]; -1 = nothing loaded
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffle: Boolean = false
) {
    val currentSong: Song? get() = queue.getOrNull(currentIndex)
    val songPath: String? get() = currentSong?.path
}

/** The part of the playback state that's worth keeping between app launches. */
data class SavedPlayback(
    val queue: List<Song>,
    val originalQueue: List<Song>,
    val currentIndex: Int,
    val positionMs: Long,
    val shuffle: Boolean
)
