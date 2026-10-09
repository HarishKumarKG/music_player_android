package com.harish.mediaplayer.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentUris
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.PowerManager
import androidx.core.content.ContextCompat
import android.graphics.drawable.Icon
import android.media.MediaMetadata
import android.media.MediaPlayer
import android.media.session.MediaSession
import android.media.session.PlaybackState as SessionPlaybackState
import android.os.Bundle
import android.os.IBinder
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import androidx.annotation.DrawableRes
import androidx.core.app.ServiceCompat
import com.harish.mediaplayer.MainActivity
import com.harish.mediaplayer.R
import com.harish.mediaplayer.data.playback.PlaybackPersistence
import com.harish.mediaplayer.domain.model.Song
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
 * artwork + previous / play-pause / next / stop.
 *
 * Uses Android's built-in MediaSession + Notification.MediaStyle (minSdk 30),
 * so no deprecated androidx.media "compat" classes are needed. The session makes the
 * same controls work on the lock screen, Bluetooth/headset buttons and the media panel.
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
        const val ACTION_SLEEP_TIMER = "com.harish.mediaplayer.SLEEP_TIMER"
        const val EXTRA_SLEEP_MINUTES = "sleep_minutes"
        private const val SLEEP_FADE_MS = 30_000L // fade the volume out over the last 30 seconds

        private const val CHANNEL_ID = "media_playback_channel"
        private const val NOTIFICATION_ID = 1
        private const val CUSTOM_ACTION_STOP = "stop"
        private const val CUSTOM_ACTION_REWIND = "rewind_10"
        private const val CUSTOM_ACTION_FORWARD = "forward_10"
        private const val ACTION_REWIND = "com.harish.mediaplayer.REWIND"
        private const val ACTION_FORWARD = "com.harish.mediaplayer.FORWARD"
        private const val SKIP_MS = 10_000L // size of one rewind / forward jump
        private const val TAG = "MediaPlayerService"
        private const val SAVE_EVERY_TICKS = 10 // 10 x 500ms = save position every 5s while playing
    }

    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false
    private var artwork: Bitmap? = null      // artwork of the current song (for notification)
    private var artworkSongId: Long? = null

    // Main dispatcher: MediaPlayer should be touched from one thread
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var progressJob: Job? = null

    private lateinit var session: MediaSession

    // ---- long-session reliability
    private lateinit var audioManager: AudioManager
    private var resumeAfterFocusGain = false   // paused by a phone call etc., not by the user
    private var consecutiveErrors = 0          // stop skipping if every song in the queue fails

    // ---- sleep timer
    private var sleepJob: Job? = null
    private var stopAfterCurrentSong = false
    private var volume = 1f                    // lowered during the sleep fade-out

    /**
     * Where a seek is going while MediaPlayer is still getting there. seekTo() is asynchronous:
     * until it finishes, currentPosition still reports the OLD time, which made the notification's
     * seek bar jump back. While this is set we report the target instead.
     */
    private var pendingSeekMs: Long? = null

    /** Position to show everywhere (UI, notification, lock screen). */
    private val shownPositionMs: Long
        get() = pendingSeekMs ?: mediaPlayer?.currentPosition?.toLong() ?: PlaybackStateHolder.state.value.positionMs

    /** Phone call, alarm, another music app...: pause and (for short interruptions) resume after. */
    private val focusRequest by lazy {
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(musicAttributes)
            .setOnAudioFocusChangeListener { change ->
                when (change) {
                    AudioManager.AUDIOFOCUS_LOSS -> { resumeAfterFocusGain = false; pause() }  // another app took over
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {                                // call / voice note
                        if (isPlaying) { resumeAfterFocusGain = true; pause() }
                    }
                    AudioManager.AUDIOFOCUS_GAIN -> {
                        if (resumeAfterFocusGain) { resumeAfterFocusGain = false; resumeOrPlay() }
                    }
                    // AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK: Android lowers our volume automatically (8.0+)
                }
            }
            .build()
    }

    private val musicAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()

    /** Headphones unplugged / Bluetooth disconnected -> pause (don't blast the speaker). */
    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY && isPlaying) pause()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        audioManager = getSystemService(AudioManager::class.java)
        ContextCompat.registerReceiver(
            this, noisyReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        session = MediaSession(this, "HarishPlayer").apply {
            // Lock screen / headset / system media panel buttons end up here
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() = resumeOrPlay()
                override fun onPause() = pause()
                override fun onSkipToNext() = next()
                override fun onSkipToPrevious() = previous()
                override fun onSeekTo(pos: Long) = seekTo(pos)
                override fun onStop() = stopPlayback()
                override fun onRewind() = skipBy(-SKIP_MS)       // headset / car "rewind"
                override fun onFastForward() = skipBy(SKIP_MS)   // headset / car "fast forward"
                override fun onCustomAction(action: String, extras: Bundle?) {
                    when (action) {
                        CUSTOM_ACTION_REWIND -> skipBy(-SKIP_MS)
                        CUSTOM_ACTION_FORWARD -> skipBy(SKIP_MS)
                        CUSTOM_ACTION_STOP -> stopPlayback()
                    }
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
            ACTION_REWIND -> skipBy(-SKIP_MS)
            ACTION_FORWARD -> skipBy(SKIP_MS)
            ACTION_SLEEP_TIMER -> setSleepTimer(intent.getIntExtra(EXTRA_SLEEP_MINUTES, PlayerController.SLEEP_OFF))
        }
        return START_NOT_STICKY
    }

    // ---------------------------------------------------------------- playback

    private val currentSong: Song? get() = PlaybackStateHolder.state.value.currentSong

    /** Start the song at the queue's current index (optionally from [startAtMs]). */
    private fun playCurrent(startAtMs: Long = 0L) {
        val song = currentSong ?: return
        pendingSeekMs = null
        mediaPlayer?.release() // always release the old player (no leaks)
        mediaPlayer = null

        val player = MediaPlayer().apply {
            // Partial wake lock while playing: with the screen off the CPU can't fall asleep,
            // e.g. in the gap between one song ending and the next starting.
            setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
            setAudioAttributes(musicAttributes)
        }
        try {
            player.setDataSource(song.path)
            player.prepare()
        } catch (e: Exception) {
            Log.e(TAG, "Can't play ${song.path}", e)
            player.release()
            skipBrokenSong()
            return
        }
        if (startAtMs > 0) player.seekTo(startAtMs, MediaPlayer.SEEK_CLOSEST)
        player.setOnCompletionListener {
            // "End of current song" sleep timer -> stop here; otherwise auto-play the next song
            if (stopAfterCurrentSong) goToSleep() else next()
        }
        player.setVolume(volume, volume) // keep the fade going across a song change
        player.setOnSeekCompleteListener {
            pendingSeekMs = null
            onStateChanged() // re-sync with the real position once the seek has landed
        }
        player.setOnErrorListener { _, what, extra ->
            Log.e(TAG, "Playback error what=$what extra=$extra on ${song.path}")
            skipBrokenSong()
            true // handled: don't let MediaPlayer call onCompletion as well
        }
        if (!requestAudioFocus()) {             // e.g. during a phone call
            player.release()
            return
        }
        consecutiveErrors = 0
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
            // Nothing loaded yet, e.g. the app was restarted and the last song was restored:
            // continue from the saved position instead of the beginning
            playCurrent(startAtMs = PlaybackStateHolder.state.value.positionMs)
        } else {
            if (!requestAudioFocus()) return
            player.start()
            isPlaying = true
            onStateChanged()
            startProgressUpdates()
        }
    }

    private fun requestAudioFocus(): Boolean =
        audioManager.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED

    /**
     * A deleted / corrupt file shouldn't end a long listening session: skip to the next song.
     * If every song in the queue fails, stop instead of looping forever.
     */
    private fun skipBrokenSong() {
        val queueSize = PlaybackStateHolder.state.value.queue.size
        if (++consecutiveErrors >= queueSize) {
            consecutiveErrors = 0
            stopPlayback()
        } else {
            next()
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
        val player = mediaPlayer
        if (player == null) {
            // Restored song not loaded yet: just remember where to start
            PlaybackStateHolder.update { it.copy(positionMs = positionMs) }
            PlaybackPersistence.save(this)
            return
        }
        pendingSeekMs = positionMs
        // SEEK_CLOSEST = land exactly where the user dropped the bar (not the nearest key frame)
        player.seekTo(positionMs, MediaPlayer.SEEK_CLOSEST)
        onStateChanged() // notification/UI show the new spot immediately
    }

    /** Jump back / forward inside the song (the notification's ⏪ 10s / ⏩ 10s). */
    private fun skipBy(deltaMs: Long) {
        val duration = mediaPlayer?.duration?.toLong()?.takeIf { it > 0 } ?: currentSong?.durationMs ?: return
        seekTo((shownPositionMs + deltaMs).coerceIn(0L, (duration - 1_000L).coerceAtLeast(0L)))
    }

    /**
     * Start / change / cancel the sleep timer.
     * The timer runs here in the service, so it works with the app closed and the screen off
     * (the wake lock keeps the CPU awake while music plays).
     */
    private fun setSleepTimer(minutes: Int) {
        sleepJob?.cancel()
        sleepJob = null
        stopAfterCurrentSong = false
        setVolume(1f)
        when {
            minutes == PlayerController.SLEEP_END_OF_SONG -> {
                stopAfterCurrentSong = true
                PlaybackStateHolder.update { it.copy(sleepTimerEndsAt = null, sleepAfterCurrentSong = true) }
            }
            minutes > 0 -> {
                val totalMs = minutes * 60_000L
                PlaybackStateHolder.update {
                    it.copy(sleepTimerEndsAt = System.currentTimeMillis() + totalMs, sleepAfterCurrentSong = false)
                }
                sleepJob = scope.launch {
                    delay((totalMs - SLEEP_FADE_MS).coerceAtLeast(0L))
                    // Gentle fade-out instead of an abrupt stop
                    val steps = 30
                    repeat(steps) { step ->
                        setVolume(1f - (step + 1f) / steps)
                        delay(SLEEP_FADE_MS / steps)
                    }
                    goToSleep()
                }
            }
            else -> PlaybackStateHolder.update { it.copy(sleepTimerEndsAt = null, sleepAfterCurrentSong = false) }
        }
    }

    private fun setVolume(value: Float) {
        volume = value
        mediaPlayer?.setVolume(value, value)
    }

    /** Timer finished: stop the music, remove the notification and close the app. */
    private fun goToSleep() {
        sleepJob = null
        stopAfterCurrentSong = false
        stopPlayback()
        PlaybackStateHolder.requestClose() // MainActivity closes itself if it's open
    }

    /** Stop button: end playback, remove the notification, hide the mini player. */
    private fun stopPlayback() {
        PlaybackPersistence.save(this) // keep the spot so the next launch can continue
        sleepJob?.cancel()             // stopping by hand also cancels the sleep timer
        sleepJob = null
        stopAfterCurrentSong = false
        volume = 1f
        stopProgressUpdates()
        mediaPlayer?.release()
        mediaPlayer = null
        isPlaying = false
        PlaybackStateHolder.clear()
        audioManager.abandonAudioFocusRequest(focusRequest)
        session.setPlaybackState(
            SessionPlaybackState.Builder().setState(SessionPlaybackState.STATE_STOPPED, 0, 0f).build()
        )
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // ------------------------------------------------------------ state + UI

    /** Push state to the app UI, the MediaSession, the notification and the save file. */
    private fun onStateChanged() {
        publishState()
        updateSession()
        currentSong?.let { goForeground(it) }
        PlaybackPersistence.save(this)
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
                positionMs = shownPositionMs,
                durationMs = player?.duration?.toLong() ?: it.durationMs
            )
        }
    }

    /** While playing, update the position twice a second (and save it every 5s). */
    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = scope.launch {
            var ticks = 0
            while (isActive) {
                publishState()
                if (++ticks % SAVE_EVERY_TICKS == 0) PlaybackPersistence.save(this@MediaPlayerService)
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
            MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, song.title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, song.artist)
                .putString(MediaMetadata.METADATA_KEY_ALBUM, song.album)
                .putLong(MediaMetadata.METADATA_KEY_DURATION, player?.duration?.toLong() ?: song.durationMs)
                .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, artwork.takeIf { artworkSongId == song.id })
                .build()
        )
        session.setPlaybackState(
            SessionPlaybackState.Builder()
                .setActions(
                    SessionPlaybackState.ACTION_PLAY or SessionPlaybackState.ACTION_PAUSE or
                        SessionPlaybackState.ACTION_PLAY_PAUSE or SessionPlaybackState.ACTION_SKIP_TO_NEXT or
                        SessionPlaybackState.ACTION_SKIP_TO_PREVIOUS or SessionPlaybackState.ACTION_SEEK_TO or
                        SessionPlaybackState.ACTION_REWIND or SessionPlaybackState.ACTION_FAST_FORWARD or
                        SessionPlaybackState.ACTION_STOP
                )
                // Android 13+ builds the media controls from the session, so Stop is added here too
                // Android 13+ builds the media controls from the session and shows two extra
                // buttons beside ⏮ ⏯ ⏭: big, easy ⏪ 10s / ⏩ 10s instead of the thin seek bar.
                // (Stop: pause, then swipe the player away.)
                .addCustomAction(
                    SessionPlaybackState.CustomAction.Builder(CUSTOM_ACTION_REWIND, "Rewind 10 seconds", R.drawable.baseline_replay_10_24).build()
                )
                .addCustomAction(
                    SessionPlaybackState.CustomAction.Builder(CUSTOM_ACTION_FORWARD, "Forward 10 seconds", R.drawable.baseline_forward_10_24).build()
                )
                .setState(
                    if (isPlaying) SessionPlaybackState.STATE_PLAYING else SessionPlaybackState.STATE_PAUSED,
                    shownPositionMs,
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

    private fun action(@DrawableRes icon: Int, title: String, intent: PendingIntent): Notification.Action =
        Notification.Action.Builder(Icon.createWithResource(this, icon), title, intent).build()

    private fun buildNotification(song: Song): Notification {
        val playPause = if (isPlaying) {
            action(R.drawable.baseline_pause_24, "Pause", serviceIntent(ACTION_TOGGLE, 1))
        } else {
            action(R.drawable.baseline_play_arrow_24, "Play", serviceIntent(ACTION_TOGGLE, 1))
        }

        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.baseline_music_note_24)
            .setContentTitle(song.title)
            .setContentText(song.artist)
            .setLargeIcon(artwork.takeIf { artworkSongId == song.id }) // song thumbnail
            .setContentIntent(openAppIntent())                         // tap -> open the app
            .setDeleteIntent(serviceIntent(ACTION_STOP, 4))            // swiped away (when paused) -> stop
            .setVisibility(Notification.VISIBILITY_PUBLIC)             // show controls on lock screen
            .setOnlyAlertOnce(true)
            .setOngoing(isPlaying)
            // Android 11-12 draw these buttons themselves: ⏮ ⏪10 ⏯ ⏩10 ⏭
            .addAction(action(R.drawable.baseline_skip_previous_24, "Previous", serviceIntent(ACTION_PREVIOUS, 0)))   // 0
            .addAction(action(R.drawable.baseline_replay_10_24, "Rewind 10 seconds", serviceIntent(ACTION_REWIND, 5)))  // 1
            .addAction(playPause)                                                                                   // 2
            .addAction(action(R.drawable.baseline_forward_10_24, "Forward 10 seconds", serviceIntent(ACTION_FORWARD, 6))) // 3
            .addAction(action(R.drawable.baseline_skip_next_24, "Next", serviceIntent(ACTION_NEXT, 2)))             // 4
            .setStyle(
                Notification.MediaStyle()
                    .setMediaSession(session.sessionToken)
                    .setShowActionsInCompactView(0, 2, 4) // prev, play/pause, next when collapsed
            )
            .build()
    }

    override fun onDestroy() {
        PlaybackPersistence.save(this) // last chance to remember the position
        unregisterReceiver(noisyReceiver)
        audioManager.abandonAudioFocusRequest(focusRequest)
        scope.cancel()
        mediaPlayer?.release()
        mediaPlayer = null
        session.release()
        PlaybackStateHolder.clear()
        super.onDestroy()
    }
}
