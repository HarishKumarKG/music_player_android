package com.harish.mediaplayer.ui.common

/** 75_000 -> "1:15", 3_725_000 -> "1:02:05" */
internal fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%d:%02d".format(minutes, seconds)
}

/** 1 -> "1 song", 5 -> "5 songs" */
fun songCount(n: Int) = if (n == 1) "1 song" else "$n songs"
