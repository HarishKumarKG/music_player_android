package com.harish.mediaplayer.cast.googlecast

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.mediarouter.media.MediaRouteSelector
import androidx.mediarouter.media.MediaRouter
import com.google.android.gms.cast.CastMediaControlIntent
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaSeekOptions
import com.google.android.gms.cast.MediaStatus
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.google.android.gms.common.api.PendingResult
import com.google.android.gms.common.api.Result
import com.google.android.gms.common.images.WebImage
import com.harish.mediaplayer.cast.CastDevice
import com.harish.mediaplayer.cast.CastException
import com.harish.mediaplayer.cast.CastKind
import com.harish.mediaplayer.cast.CastMedia
import com.harish.mediaplayer.cast.CastRenderer
import com.harish.mediaplayer.cast.RemoteState
import com.harish.mediaplayer.cast.RemoteStatus
import kotlin.coroutines.resume
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/**
 * Finds Chromecasts / Google TVs with Android's MediaRouter.
 * Everything here runs on the main thread (Cast and MediaRouter require it).
 * Phones without Google Play services simply find no Cast devices.
 */
class GoogleCastDiscovery(private val context: Context) {

    private val appId = CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID
    private val selector = MediaRouteSelector.Builder()
        .addControlCategory(CastMediaControlIntent.categoryForCast(appId))
        .build()
    private var router: MediaRouter? = null
    private var callback: MediaRouter.Callback? = null

    /** Null when Play services / Cast isn't available on this phone. */
    val castContext: CastContext? by lazy {
        try {
            @Suppress("DEPRECATION")
            CastContext.getSharedInstance(context)
        } catch (e: Exception) {
            Log.i(TAG, "Google Cast not available: ${e.message}")
            null
        }
    }

    fun start(onChanged: (List<CastDevice>) -> Unit) {
        if (castContext == null) return
        val mediaRouter = MediaRouter.getInstance(context)
        router = mediaRouter
        val cb = object : MediaRouter.Callback() {
            override fun onRouteAdded(router: MediaRouter, route: MediaRouter.RouteInfo) = onChanged(devices())
            override fun onRouteRemoved(router: MediaRouter, route: MediaRouter.RouteInfo) = onChanged(devices())
            override fun onRouteChanged(router: MediaRouter, route: MediaRouter.RouteInfo) = onChanged(devices())
        }
        callback = cb
        mediaRouter.addCallback(selector, cb, MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY)
        onChanged(devices())
    }

    fun stop() {
        callback?.let { router?.removeCallback(it) }
        callback = null
    }

    private fun routes(): List<MediaRouter.RouteInfo> =
        router?.routes.orEmpty().filter { !it.isDefault && it.isEnabled && it.matchesSelector(selector) }

    private fun devices(): List<CastDevice> = routes().map { route ->
        CastDevice(id = route.id, name = route.name, kind = CastKind.GOOGLE_CAST, description = route.description)
    }

    fun renderer(device: CastDevice): CastRenderer = GoogleCastRenderer(device, this)

    internal fun select(deviceId: String): Boolean {
        val mediaRouter = router ?: MediaRouter.getInstance(context).also { router = it }
        val route = mediaRouter.routes.firstOrNull { it.id == deviceId } ?: return false
        mediaRouter.selectRoute(route)
        return true
    }

    private companion object {
        const val TAG = "GoogleCast"
    }
}

/** Remote control for one Chromecast / Google TV session. */
private class GoogleCastRenderer(
    override val device: CastDevice,
    private val discovery: GoogleCastDiscovery
) : CastRenderer {

    private var session: CastSession? = null
    private val client: RemoteMediaClient
        get() = session?.remoteMediaClient ?: throw CastException("Lost the connection to ${device.name}")

    override suspend fun connect() = withContext(Dispatchers.Main) {
        val cast = discovery.castContext ?: throw CastException("Google Cast isn't available on this phone")
        val manager = cast.sessionManager
        manager.currentCastSession?.takeIf { it.isConnected }?.let { session = it; return@withContext }

        val result = CompletableDeferred<CastSession>()
        val listener = object : SessionManagerListener<CastSession> {
            override fun onSessionStarted(s: CastSession, sessionId: String) { result.complete(s) }
            override fun onSessionResumed(s: CastSession, wasSuspended: Boolean) { result.complete(s) }
            override fun onSessionStartFailed(s: CastSession, error: Int) {
                result.completeExceptionally(CastException("${device.name} didn't accept the connection (code $error)"))
            }
            override fun onSessionStarting(s: CastSession) = Unit
            override fun onSessionEnding(s: CastSession) = Unit
            override fun onSessionEnded(s: CastSession, error: Int) = Unit
            override fun onSessionResuming(s: CastSession, sessionId: String) = Unit
            override fun onSessionResumeFailed(s: CastSession, error: Int) = Unit
            override fun onSessionSuspended(s: CastSession, reason: Int) = Unit
        }
        manager.addSessionManagerListener(listener, CastSession::class.java)
        try {
            if (!discovery.select(device.id)) throw CastException("${device.name} is no longer available")
            session = withTimeout(20_000) { result.await() }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            throw CastException("${device.name} didn't answer. Is it on?")
        } finally {
            manager.removeSessionManagerListener(listener, CastSession::class.java)
        }
    }

    override suspend fun load(media: CastMedia, startPositionMs: Long) = withContext(Dispatchers.Main) {
        // Shown on the TV: title, artist, album and the cover picture
        val metadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MUSIC_TRACK).apply {
            putString(MediaMetadata.KEY_TITLE, media.title)
            putString(MediaMetadata.KEY_ARTIST, media.artist)
            putString(MediaMetadata.KEY_ALBUM_TITLE, media.album)
            media.artUrl?.let { addImage(WebImage(Uri.parse(it))) }
        }
        val info = MediaInfo.Builder(media.url)
            .setContentUrl(media.url)
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setContentType(media.mimeType)
            .setStreamDuration(media.durationMs)
            .setMetadata(metadata)
            .build()
        val request = MediaLoadRequestData.Builder()
            .setMediaInfo(info)
            .setAutoplay(true)
            .setCurrentTime(startPositionMs)
            .build()
        await(client.load(request), "start the song")
    }

    override suspend fun play() = withContext(Dispatchers.Main) { await(client.play(), "play") }
    override suspend fun pause() = withContext(Dispatchers.Main) { await(client.pause(), "pause") }

    override suspend fun seekTo(positionMs: Long) = withContext(Dispatchers.Main) {
        await(client.seek(MediaSeekOptions.Builder().setPosition(positionMs).build()), "seek")
    }

    override suspend fun setVolume(volume: Int) = withContext(Dispatchers.Main) {
        session?.volume = volume.coerceIn(0, 100) / 100.0
    }

    override suspend fun status(): RemoteStatus? = withContext(Dispatchers.Main) {
        val s = session ?: return@withContext null
        val c = s.remoteMediaClient ?: return@withContext null
        val state = when {
            c.isPlaying -> RemoteState.PLAYING
            c.isPaused -> RemoteState.PAUSED
            c.isBuffering || c.isLoadingNextItem -> RemoteState.BUFFERING
            c.playerState == MediaStatus.PLAYER_STATE_IDLE && c.idleReason == MediaStatus.IDLE_REASON_FINISHED -> RemoteState.FINISHED
            c.playerState == MediaStatus.PLAYER_STATE_IDLE && c.idleReason == MediaStatus.IDLE_REASON_ERROR -> RemoteState.ERROR
            else -> RemoteState.IDLE
        }
        RemoteStatus(
            state = state,
            positionMs = c.approximateStreamPosition,
            durationMs = c.streamDuration,
            volume = (s.volume * 100).toInt()
        )
    }

    override suspend fun disconnect() = withContext(Dispatchers.Main) {
        runCatching { session?.remoteMediaClient?.stop() }
        discovery.castContext?.sessionManager?.endCurrentSession(true)
        session = null
    }

    /** Waits for a Cast command's answer and turns a failure into a readable message. */
    private suspend fun <R : Result> await(pending: PendingResult<R>, what: String) {
        val ok = suspendCancellableCoroutine { cont ->
            pending.setResultCallback { result -> if (cont.isActive) cont.resume(result.status.isSuccess) }
        }
        if (!ok) throw CastException("${device.name} couldn't $what. It may not support this song's format")
    }
}
