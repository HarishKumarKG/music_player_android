package com.harish.mediaplayer

import android.app.Application
import com.harish.mediaplayer.data.playback.PlaybackPersistence
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class MediaPlayerApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // Bring back the last song / queue so the mini player is ready (paused) on launch
        appScope.launch { PlaybackPersistence.restore(this@MediaPlayerApp) }
    }
}
