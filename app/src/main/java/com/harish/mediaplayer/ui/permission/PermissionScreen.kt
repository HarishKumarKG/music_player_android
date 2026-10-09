package com.harish.mediaplayer.ui.permission

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.harish.mediaplayer.ui.navigation.AppNavHost
import com.harish.mediaplayer.ui.theme.BrandBackground
import com.harish.mediaplayer.ui.theme.MediaPlayerTheme

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
private fun PermissionRequestScreenPreview() {
    MediaPlayerTheme(darkTheme = false) {
        PermissionRequestScreen(deniedOnce = false, onGrantClick = {}, onOpenSettingsClick = {})
    }
}
