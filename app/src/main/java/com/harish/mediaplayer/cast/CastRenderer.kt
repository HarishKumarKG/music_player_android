package com.harish.mediaplayer.cast

/**
 * One TV the app is casting to. Google Cast and DLNA speak very different languages,
 * but the rest of the app only sees this small remote control.
 */
interface CastRenderer {
    val device: CastDevice

    /** Opens the connection (Google Cast starts a session on the TV; DLNA has nothing to open). */
    suspend fun connect()

    /** Starts [media] (one song) on the TV, beginning at [startPositionMs]. */
    suspend fun load(media: CastMedia, startPositionMs: Long)

    suspend fun play()
    suspend fun pause()
    suspend fun seekTo(positionMs: Long)

    /** Volume 0..100 on the TV itself. */
    suspend fun setVolume(volume: Int)

    /** Asked about once a second while casting. null = the TV didn't answer this time. */
    suspend fun status(): RemoteStatus?

    /** Stops playback on the TV and closes the connection. */
    suspend fun disconnect()
}
