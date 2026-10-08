package com.harish.mediaplayer.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentUris
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.os.IBinder
import android.provider.MediaStore
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.util.Size
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import android.content.pm.ServiceInfo
import androidx.media.app.NotificationCompat.MediaStyle
import com.harish.mediaplayer.MainActivity
import com.harish.mediaplayer.R
import com.harish.mediaplayer.screen.main.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Plays the queue in PlaybackStateHolder and shows a media notification with
 * artwork + previous / play-pause / next / stop. A MediaSession makes the same
 * controls work on the lock screen, Bluetooth headsets and Android's media panel.
 */
class MediaPlayerService : Service() {

    companion object {
        const val ACTION_PLAY_CURRENT = "com.harish.mediaplayer.PLAY_CURRENT"
        const val ACTION_TOGGLE = "com.harish.mediaplayer.TOGGLE"
        const val ACTION_NEXT = "com.harish.mediaplayer.NEXT"
        const val ACTION_PREVIOUS = "com.harish.mediaplayer.PREVIOUS"
        const val ACTION_SEEK = "com.harish.mediaplayer.SEEK"
        const val ACTION_STOP = "com.harish.mediaplayer.STOP"
        const val EXTRA_POSITION = "position_ms"

        private const val CHANNEL_ID = "media_playback_channel"
        private const val NOTIFICATION_ID = 1
        private const val CUSTOM_ACTION_STOP = "stop"
    }

    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false
    private var artwork: Bitmap? = null      // artwork of the current song (for notification)
    private var artworkSongId: Long? = null

    // Main dispatcher: MediaPlayer should be touched from one thread
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var progressJob: Job? = null

    private lateinit var session: MediaSessionCompat

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        session = MediaSessionCompat(this, "HarishPlayer").apply {
            // Lock screen / headset / system media panel buttons end up here
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() = resumeOrPlay()
                override fun onPause() = pause()
                override fun onSkipToNext() = next()
                override fun onSkipToPrevious() = previous()
                override fun onSeekTo(pos: Long) = seekTo(pos)
                override fun onStop() = stopPlayback()
                override fun onCustomAction(action: String?, extras: android.os.Bundle?) {
                    if (action == CUSTOM_ACTION_STOP) stopPlayback()
                }
            })
            setSessionActivity(openAppIntent())
            isActive = true
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_CURRENT -> playCurrent()
            ACTION_TOGGLE -> if (isPlaying) pause() else resumeOrPlay()
            ACTION_NEXT -> next()
            ACTION_PREVIOUS -> previous()
            ACTION_SEEK -> seekTo(intent.getLongExtra(EXTRA_POSITION, 0L))
            ACTION_STOP -> stopPlayback()
        }
        return START_NOT_STICKY
    }

    // ---------------------------------------------------------------- playback

    private val currentSong: Song? get() = PlaybackStateHolder.state.value.currentSong

    /** Start the song at the queue's current index from the beginning. */
    private fun playCurrent() {
        val song = currentSong ?: return
        mediaPlayer?.release() // always release the old player (no leaks)
        mediaPlayer = null

        val player = MediaPlayer()
        try {
            player.setDataSource(song.path)
            player.prepare()
        } catch (e: Exception) {
            Log.e("MediaPlayerService", "Can't play ${song.path}", e)
            player.release()
            return
        }
        player.setOnCompletionListener { next() } // auto-play the next song in the queue
        mediaPlayer = player
        isPlaying = true
        // Android 17 (API 37) only allows background audio from a running foreground
        // service, so become one *before* the sound starts.
        goForeground(song)
        player.start()
        onStateChanged()
        startProgressUpdates()
        loadArtwork(song)
    }

    private fun resumeOrPlay() {
        val player = mediaPlayer
        if (player == null) {
            playCurrent()
        } else {
            player.start()
            isPlaying = true
            onStateChanged()
            startProgressUpdates()
        }
    }

    private fun pause() {
        mediaPlayer?.pause()
        isPlaying = false
        stopProgressUpdates()
        onStateChanged()
    }

    /** Next in the queue; wraps around to the first song at the end. */
    private fun next() {
        val queue = PlaybackStateHolder.state.value.queue
        if (queue.isEmpty()) return
        val index = PlaybackStateHolder.state.value.currentIndex
        PlaybackStateHolder.setIndex((index + 1).mod(queue.size))
        playCurrent()
    }

    /** Like most players: restart the song if we're more than 3s in, else go back one. */
    private fun previous() {
        val queue = PlaybackStateHolder.state.value.queue
        if (queue.isEmpty()) return
        if ((mediaPlayer?.currentPosition ?: 0) > 3000) {
            seekTo(0)
            return
        }
        val index = PlaybackStateHolder.state.value.currentIndex
        PlaybackStateHolder.setIndex((index - 1).mod(queue.size))
        playCurrent()
    }

    private fun seekTo(positionMs: Long) {
        mediaPlayer?.seekTo(positionMs.toInt())
        onStateChanged() // update UI + notification right away
    }

    /** Stop button: end playback, remove the notification, hide the mini player. */
    private fun stopPlayback() {
        stopProgressUpdates()
        mediaPlayer?.release()
        mediaPlayer = null
        isPlaying = false
        PlaybackStateHolder.clear()
        session.setPlaybackState(
            PlaybackStateCompat.Builder().setState(PlaybackStateCompat.STATE_STOPPED, 0, 0f).build()
        )
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // ------------------------------------------------------------ state + UI

    /** Push state to the app UI, the MediaSession and the notification. */
    private fun onStateChanged() {
        publishState()
        updateSession()
        currentSong?.let { goForeground(it) }
    }

    private fun goForeground(song: Song) {
        ServiceCompat.startForeground(
            this, NOTIFICATION_ID, buildNotification(song),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        )
    }

    private fun publishState() {
        val player = mediaPlayer
        PlaybackStateHolder.update {
            it.copy(
                isPlaying = isPlaying,
                positionMs = player?.currentPosition?.toLong() ?: 0L,
                durationMs = player?.duration?.toLong() ?: 0L
            )
        }
    }

    /** While playing, update the position twice a second so progress bars move. */
    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                publishState()
                delay(500)
            }
        }
    }

    private fun stopProgressUpdates() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun updateSession() {
        val song = currentSong ?: return
        val player = mediaPlayer
        session.setMetadata(
            MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artist)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, song.album)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, player?.duration?.toLong() ?: song.durationMs)
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, artwork.takeIf { artworkSongId == song.id })
                .build()
        )
        session.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_PLAY_PAUSE or PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or PlaybackStateCompat.ACTION_SEEK_TO or
                        PlaybackStateCompat.ACTION_STOP
                )
                // Android 13+ builds the media controls from the session, so Stop is added here too
                .addCustomAction(CUSTOM_ACTION_STOP, "Stop", R.drawable.baseline_close_24)
                .setState(
                    if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                    player?.currentPosition?.toLong() ?: 0L,
                    if (isPlaying) 1f else 0f // lets the system move the seek bar by itself
                )
                .build()
        )
    }

    /** Load the album art off the main thread, then refresh session + notification. */
    private fun loadArtwork(song: Song) {
        if (artworkSongId == song.id) return
        artwork = null
        artworkSongId = song.id
        scope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                try {
                    val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.id)
                    contentResolver.loadThumbnail(uri, Size(512, 512), null)
                } catch (e: Exception) {
                    null // no embedded art
                }
            }
            if (artworkSongId == song.id && currentSong?.id == song.id) {
                artwork = bitmap
                onStateChanged()
            }
        }
    }

    // ---------------------------------------------------------- notification

    private fun createChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Media Playback", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun serviceIntent(action: String, requestCode: Int): PendingIntent =
        PendingIntent.getService(
            this, requestCode,
            Intent(this, MediaPlayerService::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun openAppIntent(): PendingIntent =
        PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun buildNotification(song: Song): Notification {
        val playPause = if (isPlaying) {
            NotificationCompat.Action(R.drawable.baseline_pause_24, "Pause", serviceIntent(ACTION_TOGGLE, 1))
        } else {
            NotificationCompat.Action(R.drawable.baseline_play_arrow_24, "Play", serviceIntent(ACTION_TOGGLE, 1))
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.baseline_music_note_24)
            .setContentTitle(song.title)
            .setContentText(song.artist)
            .setLargeIcon(artwork.takeIf { artworkSongId == song.id }) // song thumbnail
            .setContentIntent(openAppIntent())                         // tap -> open the app
            .setDeleteIntent(serviceIntent(ACTION_STOP, 4))            // swiped away (when paused) -> stop
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)       // show controls on lock screen
            .setOnlyAlertOnce(true)
            .setOngoing(isPlaying)
            .addAction(R.drawable.baseline_skip_previous_24, "Previous", serviceIntent(ACTION_PREVIOUS, 0)) // 0
            .addAction(playPause)                                                                        // 1
            .addAction(R.drawable.baseline_skip_next_24, "Next", serviceIntent(ACTION_NEXT, 2))           // 2
            .addAction(R.drawable.baseline_close_24, "Stop", serviceIntent(ACTION_STOP, 3))               // 3
            .setStyle(
                MediaStyle()
                    .setMediaSession(session.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2) // prev, play/pause, next when collapsed
            )
            .build()
    }

    override fun onDestroy() {
        scope.cancel()
        mediaPlayer?.release()
        mediaPlayer = null
        session.release()
        PlaybackStateHolder.clear()
        super.onDestroy()
    }
}
