package com.harish.mediaplayer

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.graphics.Color as AndroidColor
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.harish.mediaplayer.navigation.AppNavHost
import com.harish.mediaplayer.ui.splash.AnimatedSplash
import com.harish.mediaplayer.ui.theme.BrandBackground
import com.harish.mediaplayer.ui.theme.MediaPlayerTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.foundation.isSystemInDarkTheme
import com.harish.mediaplayer.settings.ThemeMode
import com.harish.mediaplayer.settings.ThemeRepository
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var themeRepository: ThemeRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by themeRepository.themeMode.collectAsStateWithLifecycle()
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
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

/** Permission needed to read songs: READ_MEDIA_AUDIO on Android 13+, READ_EXTERNAL_STORAGE below. */
private val audioPermission: String
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

/** Everything we ask for in one dialog. Notifications are optional (Android 13+ only). */
private val permissionsToRequest: Array<String>
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(audioPermission, Manifest.permission.POST_NOTIFICATIONS)
    } else {
        arrayOf(audioPermission)
    }

private fun hasAudioPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, audioPermission) == PackageManager.PERMISSION_GRANTED

@Composable
fun PermissionGate() {
    val context = LocalContext.current
    var audioGranted by remember { mutableStateOf(hasAudioPermission(context)) }
    var deniedOnce by remember { mutableStateOf(false) }

    // Activity Result API: replaces requestPermissions() + onRequestPermissionsResult()
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        audioGranted = results[audioPermission] == true
        if (!audioGranted) deniedOnce = true
    }

    if (audioGranted) {
        AppNavHost()
    } else {
        BrandBackground(modifier = Modifier.fillMaxSize()) {
        PermissionRequestScreen(
            deniedOnce = deniedOnce,
            onGrantClick = { launcher.launch(permissionsToRequest) },
            onOpenSettingsClick = {
                // If the user picked "Don't allow" twice, Android stops showing the dialog,
                // so the only way back is the app's settings page.
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .setData(Uri.fromParts("package", context.packageName, null))
                )
            }
        )
        }
    }
}

@Composable
fun PermissionRequestScreen(
    deniedOnce: Boolean,
    onGrantClick: () -> Unit,
    onOpenSettingsClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding() // keep content clear of status/navigation bars
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (deniedOnce) {
                "Permission is needed to list the songs on your device."
            } else {
                "We need permission to access media files."
            },
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Button(onClick = onGrantClick) {
            Text("Grant Permission")
        }
        if (deniedOnce) {
            TextButton(onClick = onOpenSettingsClick) {
                Text("Open app settings")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MediaPlayerTheme(darkTheme = false) {
        PermissionRequestScreen(deniedOnce = false, onGrantClick = {}, onOpenSettingsClick = {})
    }
}
