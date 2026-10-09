package com.harish.mediaplayer.ui.common

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.provider.MediaStore
import android.util.LruCache
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.harish.mediaplayer.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * In-memory cache so artwork isn't decoded again every time it comes back on screen.
 * Keyed by song id + pixel size, so the small list thumbnail and the big
 * full-screen artwork don't overwrite each other (the big one stays sharp).
 */
private object ThumbnailCache {
    /**
     * At most 4 artwork decodes at once. A fast fling no longer starts dozens of decodes that
     * fight the UI thread for CPU; rows that scroll away cancel their pending load anyway.
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val dispatcher = Dispatchers.IO.limitedParallelism(4)

    private val bitmaps = object : LruCache<String, Bitmap>((Runtime.getRuntime().maxMemory() / 8).toInt()) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount // budget in bytes
    }
    private val missing = java.util.Collections.synchronizedSet(mutableSetOf<Long>())

    private fun key(id: Long, sizePx: Int) = "$id:$sizePx"

    fun get(id: Long, sizePx: Int): Bitmap? = bitmaps.get(key(id, sizePx))

    fun load(context: Context, id: Long, sizePx: Int): Bitmap? {
        get(id, sizePx)?.let { return it }
        if (id in missing) return null
        val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
        return try {
            // API 29+: MediaStore extracts the embedded album art (or the album's art) for us
            context.contentResolver.loadThumbnail(uri, Size(sizePx, sizePx), null)
                .also { bitmaps.put(key(id, sizePx), it) }
        } catch (e: Exception) {
            missing.add(id) // IOException when the file has no artwork
            null
        }
    }
}

/**
 * Album art for a song that fills [modifier], or a music-note placeholder.
 * @param sizeHint roughly how big it is on screen, so we decode a sharp enough bitmap.
 * @param showPlaceholder false = draw nothing when there's no art (used for blurred backdrops).
 */
@Composable
fun SongArtwork(
    songId: Long,
    modifier: Modifier = Modifier,
    sizeHint: Dp = 48.dp,
    cornerRadius: Dp = 8.dp,
    showPlaceholder: Boolean = true
) {
    val context = LocalContext.current
    val sizePx = with(LocalDensity.current) { sizeHint.roundToPx() }

    // key(): when the song changes, throw away the old state completely so the
    // previous song's picture can never stick around (this was the mini player bug).
    key(songId, sizePx) {
        val bitmap by produceState(initialValue = ThumbnailCache.get(songId, sizePx)) {
            if (value == null) {
                value = withContext(ThumbnailCache.dispatcher) { ThumbnailCache.load(context, songId, sizePx) }
            }
        }

        val art = bitmap
        if (art != null || showPlaceholder) Box(
            modifier = modifier
                .clip(RoundedCornerShape(cornerRadius))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (art != null) {
                Image(
                    bitmap = remember(art) { art.asImageBitmap() }, // wrap once, not on every redraw
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.baseline_music_note_24),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(if (sizeHint > 100.dp) sizeHint * 0.3f else 24.dp)
                )
            }
        }
    }
}

/** Small square thumbnail used in the list and mini player. */
@Composable
fun SongThumbnail(songId: Long, size: Dp = 48.dp) {
    SongArtwork(songId = songId, modifier = Modifier.size(size), sizeHint = size)
}
