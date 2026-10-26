package com.harish.mediaplayer.cast.dlna

import com.harish.mediaplayer.cast.CastDevice
import com.harish.mediaplayer.cast.CastException
import com.harish.mediaplayer.cast.CastMedia
import com.harish.mediaplayer.cast.CastRenderer
import com.harish.mediaplayer.cast.CastServer
import com.harish.mediaplayer.cast.RemoteState
import com.harish.mediaplayer.cast.RemoteStatus
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Remote control for a DLNA smart TV (or network speaker), using the standard UPnP "SOAP" commands:
 *
 *   SetAVTransportURI("http://phone/v/…/Trip.mkv")  -> the TV opens our link
 *   Play / Pause / Seek("01:02:03") / Stop
 *   GetPositionInfo / GetTransportInfo              -> polled once a second for the seek bar
 *   SetVolume(0..100)
 *
 * The TV decodes the file itself, so whatever it supports (4K, 8K, HDR, HEVC, AV1, MKV,
 * Dolby audio…) plays in full quality.
 */
class DlnaRenderer(private val tv: DlnaTv) : CastRenderer {

    override val device: CastDevice = tv.device

    override suspend fun connect() = Unit // DLNA has no session; each command stands alone

    override suspend fun load(media: CastMedia, startPositionMs: Long) {
        // Many TVs refuse a new link while something is playing: stop first (errors are fine)
        runCatching { transport("Stop") }
        transport(
            "SetAVTransportURI",
            "CurrentURI" to media.url,
            "CurrentURIMetaData" to didl(media)
        )
        transport("Play", "Speed" to "1")
        if (startPositionMs > 5_000) {
            // Seeking only works once the TV has actually started; wait up to ~10 s for that
            repeat(20) {
                delay(500)
                val state = runCatching { transportState() }.getOrNull()
                if (state == "PLAYING" || state == "PAUSED_PLAYBACK") {
                    runCatching { seekTo(startPositionMs) }
                    return
                }
            }
        }
    }

    override suspend fun play() = transport("Play", "Speed" to "1")
    override suspend fun pause() = transport("Pause")
    override suspend fun seekTo(positionMs: Long) = transport("Seek", "Unit" to "REL_TIME", "Target" to formatTime(positionMs))

    override suspend fun setVolume(volume: Int) {
        val url = tv.renderingUrl ?: return
        soap(url, RENDERING, "SetVolume", listOf("InstanceID" to "0", "Channel" to "Master", "DesiredVolume" to volume.coerceIn(0, 100).toString()))
    }

    override suspend fun status(): RemoteStatus? = runCatching {
        val position = soap(tv.transportUrl, TRANSPORT, "GetPositionInfo", listOf("InstanceID" to "0"))
        val state = transportState()
        val volume = tv.renderingUrl?.let { url ->
            runCatching {
                tag(soap(url, RENDERING, "GetVolume", listOf("InstanceID" to "0", "Channel" to "Master")), "CurrentVolume")?.toIntOrNull()
            }.getOrNull()
        }
        val pos = parseTime(tag(position, "RelTime"))
        val dur = parseTime(tag(position, "TrackDuration"))
        RemoteStatus(
            state = when (state) {
                "PLAYING" -> RemoteState.PLAYING
                "PAUSED_PLAYBACK", "PAUSED_RECORDING" -> RemoteState.PAUSED
                "TRANSITIONING" -> RemoteState.BUFFERING
                // A TV that stops by itself right at the end has finished the song
                "STOPPED", "NO_MEDIA_PRESENT" -> if (dur > 0 && pos >= dur - 3_000) RemoteState.FINISHED else RemoteState.IDLE
                else -> RemoteState.IDLE
            },
            positionMs = pos,
            durationMs = dur,
            volume = volume
        )
    }.getOrNull()

    override suspend fun disconnect() {
        runCatching { transport("Stop") }
    }

    private suspend fun transportState(): String? =
        tag(soap(tv.transportUrl, TRANSPORT, "GetTransportInfo", listOf("InstanceID" to "0")), "CurrentTransportState")

    private suspend fun transport(action: String, vararg args: Pair<String, String>) {
        soap(tv.transportUrl, TRANSPORT, action, listOf("InstanceID" to "0") + args)
    }

    private suspend fun soap(url: String, service: String, action: String, args: List<Pair<String, String>>): String =
        withContext(Dispatchers.IO) {
            val body = buildString {
                append("<?xml version=\"1.0\" encoding=\"utf-8\"?>")
                append("<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">")
                append("<s:Body><u:").append(action).append(" xmlns:u=\"").append(service).append("\">")
                args.forEach { (k, v) -> append('<').append(k).append('>').append(escape(v)).append("</").append(k).append('>') }
                append("</u:").append(action).append("></s:Body></s:Envelope>")
            }.toByteArray(Charsets.UTF_8)
            val conn = URL(url).openConnection() as HttpURLConnection
            try {
                conn.requestMethod = "POST"
                conn.connectTimeout = 4000
                conn.readTimeout = 6000
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "text/xml; charset=\"utf-8\"")
                conn.setRequestProperty("SOAPAction", "\"$service#$action\"")
                conn.setFixedLengthStreamingMode(body.size)
                conn.outputStream.use { it.write(body) }
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (code !in 200..299) {
                    throw CastException("${device.name} refused \"$action\" (${tag(text, "errorDescription") ?: "error $code"})")
                }
                text
            } catch (e: CastException) {
                throw e
            } catch (e: Exception) {
                throw CastException("Can't reach ${device.name}. Is it on and on the same Wi-Fi?", e)
            } finally {
                conn.disconnect()
            }
        }

    companion object {
        private const val TRANSPORT = "urn:schemas-upnp-org:service:AVTransport:1"
        private const val RENDERING = "urn:schemas-upnp-org:service:RenderingControl:1"

        /** Title and format for the TV's own screen (many Samsung/LG TVs need this to play at all). */
        internal fun didl(media: CastMedia): String {
            val protocol = "http-get:*:${media.mimeType}:${CastServer.DLNA_FEATURES}"
            val duration = if (media.durationMs > 0) " duration=\"${formatTime(media.durationMs)}.000\"" else ""
            return "<DIDL-Lite xmlns=\"urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/\" " +
                "xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:upnp=\"urn:schemas-upnp-org:metadata-1-0/upnp/\">" +
                "<item id=\"0\" parentID=\"-1\" restricted=\"1\">" +
                "<dc:title>${escape(media.title)}</dc:title>" +
                "<dc:creator>${escape(media.artist)}</dc:creator>" +
                "<upnp:artist>${escape(media.artist)}</upnp:artist>" +
                "<upnp:album>${escape(media.album)}</upnp:album>" +
                (media.artUrl?.let { "<upnp:albumArtURI>${escape(it)}</upnp:albumArtURI>" } ?: "") +
                "<upnp:class>object.item.audioItem.musicTrack</upnp:class>" +
                "<res protocolInfo=\"$protocol\" size=\"${media.sizeBytes}\"$duration>${escape(media.url)}</res>" +
                "</item></DIDL-Lite>"
        }

        internal fun escape(text: String): String = text
            .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;").replace("'", "&apos;")

        /** Text inside the first <name>…</name> (namespaces ignored), with &amp; etc. decoded. */
        internal fun tag(xml: String, name: String): String? {
            val match = Regex("<(?:\\w+:)?$name(?:\\s[^>]*)?>(.*?)</(?:\\w+:)?$name>", RegexOption.DOT_MATCHES_ALL).find(xml)
            return match?.groupValues?.get(1)?.trim()
                ?.replace("&lt;", "<")?.replace("&gt;", ">")?.replace("&quot;", "\"")?.replace("&apos;", "'")?.replace("&amp;", "&")
        }

        /** 3_723_000 ms -> "1:02:03" */
        internal fun formatTime(ms: Long): String {
            val s = (ms / 1000).coerceAtLeast(0)
            return "%d:%02d:%02d".format(s / 3600, (s % 3600) / 60, s % 60)
        }

        /** "1:02:03" or "01:02:03.500" -> ms; anything else (e.g. "NOT_IMPLEMENTED") -> 0. */
        internal fun parseTime(text: String?): Long {
            val parts = text?.substringBefore('.')?.split(':') ?: return 0
            if (parts.size != 3) return 0
            val (h, m, s) = parts.map { it.toLongOrNull() ?: return 0 }
            return ((h * 60 + m) * 60 + s) * 1000
        }
    }
}
