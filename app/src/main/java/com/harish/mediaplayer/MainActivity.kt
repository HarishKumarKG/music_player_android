package com.harish.mediaplayer

import android.app.UiModeManager
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.harish.mediaplayer.data.settings.ThemeRepository
import com.harish.mediaplayer.domain.model.ThemeMode
import com.harish.mediaplayer.playback.PlaybackStateHolder
import com.harish.mediaplayer.ui.permission.PermissionGate
import com.harish.mediaplayer.ui.splash.AnimatedSplash
import com.harish.mediaplayer.ui.theme.MediaPlayerTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var themeRepository: ThemeRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by themeRepository.themeMode.collectAsStateWithLifecycle()
            // Sleep timer finished -> close the app (and remove it from Recents)
            LaunchedEffect(Unit) {
                PlaybackStateHolder.closeRequests.collect { finishAndRemoveTask() }
            }
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            // Android 12+: tell the system our own light/dark choice. This makes the
            // *system* splash (shown before any of our code runs) use values-night colours
            // next time when the app is set to Dark, even if the phone is in light mode.
            LaunchedEffect(themeMode) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    getSystemService(UiModeManager::class.java)?.setApplicationNightMode(
                        when (themeMode) {
                            ThemeMode.SYSTEM -> UiModeManager.MODE_NIGHT_AUTO
                            ThemeMode.LIGHT -> UiModeManager.MODE_NIGHT_NO
                            ThemeMode.DARK -> UiModeManager.MODE_NIGHT_YES
                        }
                    )
                }
            }
            // Status/nav bar icons follow the APP theme (it may differ from the phone's setting)
            LaunchedEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT
                    ) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(
                        AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT
                    ) { darkTheme }
                )
            }

            MediaPlayerTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // rememberSaveable: don't replay the intro on rotation
                    var showSplash by rememberSaveable { mutableStateOf(true) }
                    AnimatedContent(
                        targetState = showSplash,
                        transitionSpec = {
                            // splash zooms out & fades while the app fades in
                            fadeIn(tween(450)) togetherWith
                                (fadeOut(tween(450)) + scaleOut(tween(450), targetScale = 1.15f))
                        },
                        label = "splash"
                    ) { splash ->
                        if (splash) AnimatedSplash(onFinished = { showSplash = false })
                        else PermissionGate()
                    }
                }
            }
        }
    }
}
