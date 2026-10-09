package com.harish.mediaplayer.util

import android.content.Context
import com.harish.mediaplayer.screen.main.model.toJsonArray
import com.harish.mediaplayer.screen.main.model.toSongs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

/**
 * Saves the last queue / song / position / shuffle to `last_playback.json`
 * in the app's private storage, and loads it back on the next launch.
 */
object PlaybackPersistence {
    private const val FILE_NAME = "last_playback.json"

    // One writer at a time, off the main thread
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val io = Dispatchers.IO.limitedParallelism(1)
    private val scope = CoroutineScope(SupervisorJob() + io)

    /** Write the current state (cheap; called on song change, pause, seek, every few seconds). */
    fun save(context: Context) {
        val saved = PlaybackStateHolder.snapshot() ?: return
        val file = File(context.applicationContext.filesDir, FILE_NAME)
        scope.launch {
            try {
                val json = JSONObject()
                    .put("queue", saved.queue.toJsonArray())
                    .put("originalQueue", saved.originalQueue.toJsonArray())
                    .put("currentIndex", saved.currentIndex)
                    .put("positionMs", saved.positionMs)
                    .put("shuffle", saved.shuffle)
                // temp file + rename, so a crash mid-write never leaves a broken file
                val tmp = File(file.parentFile, "$FILE_NAME.tmp")
                tmp.writeText(json.toString())
                tmp.renameTo(file)
            } catch (_: Exception) {
                // saving is best-effort; never crash playback because of it
            }
        }
    }

    /** Read the save file and put it back into PlaybackStateHolder (paused). */
    suspend fun restore(context: Context) = withContext(io) {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return@withContext
        try {
            val json = JSONObject(file.readText())
            val queue = json.getJSONArray("queue").toSongs()
            val index = json.getInt("currentIndex")
            // the song file may have been deleted since last time
            if (queue.getOrNull(index)?.let { File(it.path).exists() } != true) return@withContext
            PlaybackStateHolder.restore(
                SavedPlayback(
                    queue = queue,
                    originalQueue = json.getJSONArray("originalQueue").toSongs(),
                    currentIndex = index,
                    positionMs = json.getLong("positionMs"),
                    shuffle = json.getBoolean("shuffle")
                )
            )
        } catch (_: Exception) {
            file.delete() // unreadable/old format: start fresh next time
        }
    }
}
