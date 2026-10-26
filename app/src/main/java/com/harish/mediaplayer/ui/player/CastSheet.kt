package com.harish.mediaplayer.ui.player

import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.harish.mediaplayer.R
import com.harish.mediaplayer.cast.CastDevice
import com.harish.mediaplayer.cast.CastKind
import com.harish.mediaplayer.cast.TvFinder
import com.harish.mediaplayer.playback.PlaybackStateHolder
import com.harish.mediaplayer.playback.PlayerController

/** Android 17 (API 37) asks before an app may talk to devices on your Wi-Fi. */
private const val LOCAL_NETWORK_PERMISSION = "android.permission.ACCESS_LOCAL_NETWORK"

/**
 * What the Cast button does: on Android 17+ ask once for "devices on your network",
 * then open the TV list with [open].
 */
@Composable
fun rememberCastButtonClick(open: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val askLocalNetwork = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) open()
        else Toast.makeText(context, "Allow access to devices on your network to find TVs", Toast.LENGTH_LONG).show()
    }
    return {
        if (Build.VERSION.SDK_INT >= 37 &&
            context.checkSelfPermission(LOCAL_NETWORK_PERMISSION) != PackageManager.PERMISSION_GRANTED
        ) {
            askLocalNetwork.launch(LOCAL_NETWORK_PERMISSION)
        } else {
            open()
        }
    }
}

/**
 * "Play on a TV" sheet. Searches for TVs while it's open and talks to the playback service
 * through PlayerController, so it can be opened from anywhere.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CastSheetHost(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val playback by PlaybackStateHolder.state.collectAsStateWithLifecycle()
    val devices by TvFinder.devices.collectAsStateWithLifecycle()
    val searching by TvFinder.searching.collectAsStateWithLifecycle()

    // Search only while the sheet is open (it uses the Wi-Fi and battery)
    DisposableEffect(Unit) {
        TvFinder.startSearch(context)
        onDispose { TvFinder.stopSearch() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(modifier = Modifier.navigationBarsPadding().padding(bottom = 16.dp)) {
            val tvName = playback.castTvName
            if (tvName != null) {
                NowCasting(
                    tvName = tvName,
                    connecting = playback.castConnecting,
                    volume = playback.castVolume,
                    onVolume = { PlayerController.setCastVolume(context, it) },
                    onStop = {
                        PlayerController.stopCasting(context)
                        onDismiss()
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            Text(
                text = if (tvName != null) "Switch to another TV" else "Play on a TV",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            val others = devices.filter { it.name != tvName }
            others.forEach { device ->
                DeviceRow(device = device, onClick = {
                    PlayerController.castTo(context, device.id)
                    onDismiss()
                })
            }

            if (others.isEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    if (searching) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    Text(
                        text = if (searching) "Looking for TVs on your Wi-Fi…" else "No TVs found",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Text(
                    text = "Turn the TV on and connect it to the same Wi-Fi as this phone. " +
                        "Works with Chromecast / Google TV and most smart TVs (Samsung, LG, Sony…).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }
    }
}

@Composable
private fun NowCasting(
    tvName: String,
    connecting: Boolean,
    volume: Int?,
    onVolume: (Int) -> Unit,
    onStop: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painterResource(R.drawable.baseline_cast_connected_24),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(tvName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    if (connecting) "Connecting…" else "Playing on this TV",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // TV volume: shows what the slider is being dragged to, sends it when let go
        if (!connecting) {
            var dragging by remember { mutableStateOf<Float?>(null) }
            val shown = dragging ?: ((volume ?: 50) / 100f)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                IconButton(onClick = { onVolume((volume ?: 50) - 5) }) {
                    Icon(painterResource(R.drawable.baseline_volume_down_24), contentDescription = "TV volume down")
                }
                Slider(
                    value = shown,
                    onValueChange = { dragging = it },
                    onValueChangeFinished = {
                        dragging?.let { onVolume((it * 100).toInt()) }
                        dragging = null
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.tertiary,
                        activeTrackColor = MaterialTheme.colorScheme.tertiary
                    ),
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { onVolume((volume ?: 50) + 5) }) {
                    Icon(painterResource(R.drawable.baseline_volume_up_24), contentDescription = "TV volume up")
                }
            }
        }

        OutlinedButton(onClick = onStop, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Icon(painterResource(R.drawable.baseline_smartphone_24), contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Stop casting, back to the phone")
        }
    }
}

@Composable
private fun DeviceRow(device: CastDevice, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Icon(
            painterResource(if (device.kind == CastKind.GOOGLE_CAST) R.drawable.baseline_cast_24 else R.drawable.baseline_tv_24),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(device.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                device.description ?: device.kind.label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
