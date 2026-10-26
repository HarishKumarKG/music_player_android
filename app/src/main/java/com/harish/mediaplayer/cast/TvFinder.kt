package com.harish.mediaplayer.cast

import android.content.Context
import android.util.Log
import com.harish.mediaplayer.cast.dlna.DlnaRenderer
import com.harish.mediaplayer.cast.dlna.DlnaTv
import com.harish.mediaplayer.cast.dlna.SsdpDiscovery
import com.harish.mediaplayer.cast.googlecast.GoogleCastDiscovery
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Finds TVs on the Wi-Fi while the "Cast" sheet is open:
 * Google Cast devices (through Play services) and DLNA smart TVs (through SSDP) at the same time.
 *
 * It's app-wide (like PlaybackStateHolder), so the sheet fills the list and the playback
 * service turns the TV you picked into a [CastRenderer] (a remote control for it).
 */
object TvFinder {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var appContext: Context? = null
    private var googleCast: GoogleCastDiscovery? = null
    private val dlnaTvs = LinkedHashMap<String, DlnaTv>()
    private var castDevices: List<CastDevice> = emptyList()
    private var searchJob: Job? = null

    private val _devices = MutableStateFlow<List<CastDevice>>(emptyList())
    val devices: StateFlow<List<CastDevice>> = _devices.asStateFlow()

    private val _searching = MutableStateFlow(false)
    val searching: StateFlow<Boolean> = _searching.asStateFlow()

    private fun googleCast(context: Context): GoogleCastDiscovery {
        val app = context.applicationContext
        appContext = app
        return googleCast ?: GoogleCastDiscovery(app).also { googleCast = it }
    }

    /** Looks for TVs until [stopSearch] (DLNA asks again every few seconds, Cast updates live). */
    fun startSearch(context: Context) {
        if (searchJob?.isActive == true) return
        googleCast(context).start { found ->
            castDevices = found
            publish()
        }
        val ssdp = SsdpDiscovery(context.applicationContext)
        searchJob = scope.launch {
            _searching.value = true
            try {
                while (isActive) {
                    ssdp.search(timeoutMs = 4_000) { tv ->
                        scope.launch {
                            dlnaTvs[tv.device.id] = tv
                            publish()
                        }
                    }
                    delay(6_000)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "TV search failed", e)
            } finally {
                _searching.value = false
            }
        }
    }

    fun stopSearch() {
        searchJob?.cancel()
        searchJob = null
        googleCast?.stop()
    }

    fun find(deviceId: String): CastDevice? = _devices.value.firstOrNull { it.id == deviceId }

    /** A remote control for [device] (not connected yet: call [CastRenderer.connect]). */
    fun renderer(context: Context, device: CastDevice): CastRenderer = when (device.kind) {
        CastKind.GOOGLE_CAST -> googleCast(context).renderer(device)
        CastKind.DLNA -> DlnaRenderer(dlnaTvs[device.id] ?: throw CastException("${device.name} is no longer available"))
    }

    private fun publish() {
        _devices.value = (castDevices + dlnaTvs.values.map { it.device }).sortedBy { it.name.lowercase() }
    }

    private const val TAG = "TvFinder"
}
