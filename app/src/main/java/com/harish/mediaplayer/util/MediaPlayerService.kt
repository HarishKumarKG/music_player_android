package com.harish.mediaplayer.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.harish.mediaplayer.R
import java.io.IOException

class MediaPlayerService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var currentSongPath: String? = null
    private var isPlaying: Boolean = false

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        mediaPlayer = MediaPlayer()  // Initialize MediaPlayer
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val action = intent.getStringExtra("action")
        val songPath = intent.getStringExtra("song_path")

        when (action) {
            "play" -> {
                if (songPath != null) {
                    if (mediaPlayer?.isPlaying == true) {
                        mediaPlayer?.stop()
                        mediaPlayer?.release()
                    }
                    mediaPlayer = MediaPlayer()

                    try {
                        mediaPlayer?.setDataSource(songPath)
                        mediaPlayer?.prepare()
                        mediaPlayer?.start()
                        isPlaying = true
                        currentSongPath = songPath
                    } catch (e: IOException) {
                        Log.e("MediaPlayerService", "Error setting data source", e)
                    }

                    startForegroundService(songPath, true)
                } else {
                    Log.e("MediaPlayerService", "Song path is null")
                }
            }
            "pause" -> {
                mediaPlayer?.pause()
                isPlaying = false
                startForegroundService(currentSongPath ?: "", false)
            }
        }

        return START_NOT_STICKY
    }

    private fun startForegroundService(songPath: String, isPlaying: Boolean) {
        val notification = createForegroundServiceNotification(songPath, isPlaying)
        startForeground(1, notification)
    }

    private fun createForegroundServiceNotification(songPath: String, isPlaying: Boolean): Notification {
        val channelId = "media_playback_channel"
        val channelName = "Media Playback"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(notificationChannel)
        }

        // Action for play/pause button
        val playPauseAction = if (isPlaying) {
            val pauseIntent = Intent(this, MediaPlayerService::class.java).apply {
                putExtra("action", "pause")
            }
            val pausePendingIntent = PendingIntent.getService(this, 0, pauseIntent, PendingIntent.FLAG_IMMUTABLE)
            NotificationCompat.Action(R.drawable.baseline_pause_24, "Pause", pausePendingIntent)
        } else {
            val playIntent = Intent(this, MediaPlayerService::class.java).apply {
                putExtra("action", "play")
                putExtra("song_path", songPath)
            }
            val playPendingIntent = PendingIntent.getService(this, 0, playIntent, PendingIntent.FLAG_IMMUTABLE)
            NotificationCompat.Action(R.drawable.baseline_play_arrow_24, "Play", playPendingIntent)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Media Player")
            .setContentText("Playing: ${songPath}")
            .setSmallIcon(R.drawable.baseline_music_note_24)
            .setOngoing(true)
            .addAction(playPauseAction) // Add play/pause button
            .setStyle(
                NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0)  // Show only the first action (play/pause)
                    .setMediaSession(null)  // Optional, but for better media support, link a media session
            )
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
    }
}