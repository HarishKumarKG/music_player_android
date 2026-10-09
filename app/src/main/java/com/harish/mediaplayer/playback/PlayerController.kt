package com.harish.mediaplayer.playback

import android.content.Context
import android.content.Intent
import com.harish.mediaplayer.data.playback.PlaybackPersistence
import com.harish.mediaplayer.domain.model.Song

/** What the UI calls. Updates the shared queue, then tells the service what to do. */
object PlayerController {

    /** Play [songs] (the list exactly as shown, i.e. in sort order) starting at [index]. */
    fun playFromList(context: Context, songs: List<Song>, index: Int) {
        PlaybackStateHolder.setQueue(songs, index)
        send(context, MediaPlayerService.ACTION_PLAY_CURRENT)
    }

    /** Jump to a song inside the current queue (from the "Up next" sheet). */
    fun playQueueItem(context: Context, queueIndex: Int) {
        PlaybackStateHolder.setIndex(queueIndex)
        send(context, MediaPlayerService.ACTION_PLAY_CURRENT)
    }

    fun togglePlayPause(context: Context) = send(context, MediaPlayerService.ACTION_TOGGLE)
    fun next(context: Context) = send(context, MediaPlayerService.ACTION_NEXT)
    fun previous(context: Context) = send(context, MediaPlayerService.ACTION_PREVIOUS)
    fun stop(context: Context) = send(context, MediaPlayerService.ACTION_STOP)

    fun seekTo(context: Context, positionMs: Long) = context.startService(
        Intent(context, MediaPlayerService::class.java)
            .setAction(MediaPlayerService.ACTION_SEEK)
            .putExtra(MediaPlayerService.EXTRA_POSITION, positionMs)
    )

    /**
     * Sleep timer: stop (and close the app) after [minutes].
     * Use [SLEEP_END_OF_SONG] to stop when the current song ends, or [SLEEP_OFF] to cancel.
     */
    fun setSleepTimer(context: Context, minutes: Int) = context.startService(
        Intent(context, MediaPlayerService::class.java)
            .setAction(MediaPlayerService.ACTION_SLEEP_TIMER)
            .putExtra(MediaPlayerService.EXTRA_SLEEP_MINUTES, minutes)
    )

    const val SLEEP_OFF = 0
    const val SLEEP_END_OF_SONG = -1

    /** Shuffle only reorders the queue; the current song keeps playing. */
    fun toggleShuffle(context: Context) {
        PlaybackStateHolder.toggleShuffle()
        PlaybackPersistence.save(context)
    }

    private fun send(context: Context, action: String) {
        context.startService(Intent(context, MediaPlayerService::class.java).setAction(action))
    }
}
