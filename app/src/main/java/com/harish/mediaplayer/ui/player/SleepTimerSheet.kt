package com.harish.mediaplayer.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import com.harish.mediaplayer.domain.model.PlaybackState
import com.harish.mediaplayer.playback.PlaybackStateHolder
import com.harish.mediaplayer.playback.PlayerController
import com.harish.mediaplayer.ui.common.formatDuration
import kotlinx.coroutines.delay

/** Ready-made choices, in minutes. */
private val presetMinutes = listOf(15, 30, 45, 60, 120)

/**
 * Self-contained sleep timer sheet: reads the timer from PlaybackStateHolder and sets it
 * through PlayerController, so it can be opened from anywhere (player, menu...).
 */
@Composable
fun SleepTimerSheetHost(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val playback by PlaybackStateHolder.state.collectAsStateWithLifecycle()
    SleepTimerSheet(
        playback = playback,
        onSet = { minutes ->
            PlayerController.setSleepTimer(context, minutes)
            onDismiss()
        },
        onDismiss = onDismiss
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SleepTimerSheet(
    playback: PlaybackState,
    onSet: (minutes: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var showCustom by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(modifier = Modifier.navigationBarsPadding().padding(bottom = 16.dp)) {
            Row(
                modifier = Modifier.padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(painterResource(R.drawable.baseline_bedtime_24), contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Sleep timer", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        text = sleepTimerStatus(playback) ?: "Music stops and the app closes",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (playback.isSleepTimerOn) MaterialTheme.colorScheme.tertiary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (playback.isSleepTimerOn) {
                    OutlinedButton(onClick = { onSet(PlayerController.SLEEP_OFF) }) { Text("Turn off") }
                }
            }
            Spacer(Modifier.padding(top = 12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            presetMinutes.forEach { minutes -> TimerOption(formatMinutes(minutes)) { onSet(minutes) } }
            TimerOption("End of current song") { onSet(PlayerController.SLEEP_END_OF_SONG) }
            TimerOption("Custom…") { showCustom = true }
        }
    }

    if (showCustom) {
        CustomTimerDialog(
            onConfirm = { minutes ->
                showCustom = false
                onSet(minutes)
            },
            onDismiss = { showCustom = false }
        )
    }
}

@Composable
private fun TimerOption(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp)
    )
}

/** Slider from 5 minutes to 4 hours, in 5-minute steps. */
@Composable
private fun CustomTimerDialog(onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    var minutes by remember { mutableFloatStateOf(90f) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom sleep timer") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    formatMinutes(minutes.toInt()),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Slider(
                    value = minutes,
                    onValueChange = { minutes = it },
                    valueRange = 5f..240f,
                    steps = 46 // (240 - 5) / 5 - 1 -> 5-minute steps
                )
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(minutes.toInt()) }) { Text("Start") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/**
 * "Stops in 42:10" / "Stops after this song" (ticks every second), or null when the timer is off.
 * Shared by the sheet and the player's 🌙 button.
 */
@Composable
fun sleepTimerStatus(playback: PlaybackState): String? {
    val endsAt = playback.sleepTimerEndsAt
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(endsAt) {
        while (endsAt != null) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }
    return when {
        playback.sleepAfterCurrentSong -> "Stops after this song"
        endsAt != null -> "Stops in " + formatDuration((endsAt - now).coerceAtLeast(0L))
        else -> null
    }
}

/** 45 -> "45 min", 60 -> "1 hour", 90 -> "1 h 30 min", 120 -> "2 hours" */
internal fun formatMinutes(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0 -> "$m min"
        m == 0 -> if (h == 1) "1 hour" else "$h hours"
        else -> "$h h $m min"
    }
}
