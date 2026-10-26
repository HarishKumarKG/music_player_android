package com.harish.mediaplayer.cast.dlna

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import com.harish.mediaplayer.cast.CastDevice
import com.harish.mediaplayer.cast.CastKind
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

/** A DLNA TV plus the addresses of its two remote-control "services". */
data class DlnaTv(
    val device: CastDevice,
    /** AVTransport: play, pause, seek, what's playing. */
    val transportUrl: String,
    /** RenderingControl: volume (some TVs don't have it). */
    val renderingUrl: String?
)

/**
 * Finds smart TVs on the Wi-Fi with SSDP, the "who's there?" message every DLNA device answers.
 *
 * 1. We shout "any MediaRenderer here?" to the multicast address 239.255.255.250:1900.
 * 2. Each TV answers with a LOCATION link to its description XML.
 * 3. We read that XML: the TV's name, model, and the URLs used to control it.
 */
class SsdpDiscovery(private val context: Context) {

    /** Searches for [timeoutMs]; [onFound] is called for each TV as soon as it answers. */
    suspend fun search(timeoutMs: Long = 5_000, onFound: (DlnaTv) -> Unit) = withContext(Dispatchers.IO) {
        val wifi = context.applicationContext.getSystemService(WifiManager::class.java)
        // Android drops multicast packets to save battery unless an app holds this lock
        val lock = wifi?.createMulticastLock("cast-search")?.apply { setReferenceCounted(false); acquire() }
        val seen = HashSet<String>()
        try {
            DatagramSocket().use { socket ->
                socket.soTimeout = 500
                val group = InetAddress.getByName("239.255.255.250")
                listOf(RENDERER, "ssdp:all").forEach { target ->
                    val msg = "M-SEARCH * HTTP/1.1\r\nHOST: 239.255.255.250:1900\r\nMAN: \"ssdp:discover\"\r\nMX: 2\r\nST: $target\r\n\r\n"
                    val bytes = msg.toByteArray()
                    repeat(2) { socket.send(DatagramPacket(bytes, bytes.size, group, 1900)) }
                }
                val deadline = System.currentTimeMillis() + timeoutMs
                val buffer = ByteArray(4096)
                coroutineScope {
                    while (isActive && System.currentTimeMillis() < deadline) {
                        val packet = DatagramPacket(buffer, buffer.size)
                        try {
                            socket.receive(packet)
                        } catch (e: SocketTimeoutException) {
                            continue
                        }
                        val reply = String(packet.data, 0, packet.length)
                        val location = header(reply, "LOCATION") ?: continue
                        if (!seen.add(location)) continue
                        launch(Dispatchers.IO) {
                            runCatching { describe(location) }
                                .onSuccess { tv -> if (tv != null) onFound(tv) }
                                .onFailure { Log.d(TAG, "Not a usable TV at $location: ${it.message}") }
                        }
                    }
                }
            }
        } finally {
            lock?.release()
        }
    }

    /** Reads the device description XML; null if it isn't a media renderer we can control. */
    private fun describe(location: String): DlnaTv? {
        val xml = httpGet(location)
        val parser = XmlPullParserFactory.newInstance().apply { isNamespaceAware = false }.newPullParser()
        parser.setInput(xml.reader())

        var friendlyName: String? = null
        var modelName: String? = null
        var manufacturer: String? = null
        var udn: String? = null
        var urlBase: String? = null
        var deviceType: String? = null
        var transport: String? = null
        var rendering: String? = null
        var serviceType: String? = null
        var controlUrl: String? = null
        var tag = ""

        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> {
                    tag = parser.name.substringAfter(':')
                    if (tag == "service") { serviceType = null; controlUrl = null }
                }
                XmlPullParser.TEXT -> {
                    val text = parser.text.trim()
                    if (text.isNotEmpty()) when (tag) {
                        "friendlyName" -> if (friendlyName == null) friendlyName = text
                        "modelName" -> if (modelName == null) modelName = text
                        "manufacturer" -> if (manufacturer == null) manufacturer = text
                        "UDN" -> if (udn == null) udn = text
                        "URLBase" -> urlBase = text
                        "deviceType" -> if (deviceType == null || text.contains("MediaRenderer")) deviceType = text
                        "serviceType" -> serviceType = text
                        "controlURL" -> controlUrl = text
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name.substringAfter(':') == "service") {
                        val type = serviceType.orEmpty()
                        val url = controlUrl
                        if (url != null) {
                            if (type.contains(":AVTransport:") && transport == null) transport = url
                            if (type.contains(":RenderingControl:") && rendering == null) rendering = url
                        }
                    }
                    tag = ""
                }
            }
            parser.next()
        }
        val avTransport = transport ?: return null // can't play media: not a TV / renderer
        val base = urlBase ?: location
        val name = friendlyName ?: manufacturer ?: "Smart TV"
        return DlnaTv(
            device = CastDevice(
                id = udn ?: location,
                name = name,
                kind = CastKind.DLNA,
                description = listOfNotNull(manufacturer, modelName).joinToString(" ").ifBlank { null }
            ),
            transportUrl = resolve(base, avTransport),
            renderingUrl = rendering?.let { resolve(base, it) }
        )
    }

    companion object {
        private const val TAG = "SsdpDiscovery"
        private const val RENDERER = "urn:schemas-upnp-org:device:MediaRenderer:1"

        internal fun header(reply: String, name: String): String? =
            reply.lineSequence().firstOrNull { it.startsWith("$name:", ignoreCase = true) }
                ?.substringAfter(':')?.trim()

        /** Control URLs are usually relative ("/upnp/control/AVTransport1"). */
        internal fun resolve(base: String, path: String): String = URL(URL(base), path).toString()

        internal fun httpGet(url: String): String {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 3000
            conn.readTimeout = 3000
            return try {
                conn.inputStream.bufferedReader().use { it.readText() }
            } finally {
                conn.disconnect()
            }
        }
    }
}
