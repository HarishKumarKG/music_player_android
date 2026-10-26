package com.harish.mediaplayer.domain.model

/** Everything about playback the UI needs. Written by MediaPlayerService / PlayerController. */
data class PlaybackState(
    val queue: List<Song> = emptyList(), // songs in PLAY order (shuffled order when shuffle is on)
    val currentIndex: Int = -1,          // position in [queue]; -1 = nothing loaded
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffle: Boolean = false,
    /** Wall-clock time (System.currentTimeMillis) when the sleep timer stops playback; null = off. */
    val sleepTimerEndsAt: Long? = null,
    /** Sleep timer set to "end of current song". */
    val sleepAfterCurrentSong: Boolean = false,
    /** Name of the TV the music is playing on (null = playing on the phone). */
    val castTvName: String? = null,
    /** Still connecting to [castTvName]. */
    val castConnecting: Boolean = false,
    /** The TV's own volume 0..100 while casting (null = unknown). */
    val castVolume: Int? = null
) {
    val isCasting: Boolean get() = castTvName != null

    val isSleepTimerOn: Boolean get() = sleepTimerEndsAt != null || sleepAfterCurrentSong

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
