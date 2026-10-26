package com.harish.mediaplayer.cast

/** The two ways the app can send music to a TV. */
enum class CastKind(val label: String) {
    /** Chromecast, Google TV, Android TV, Nest speakers and TVs with "Chromecast built-in". */
    GOOGLE_CAST("Chromecast / Google TV"),

    /** The media player built into most smart TVs (Samsung, LG, Sony, Panasonic…). */
    DLNA("Smart TV (DLNA)")
}

/** A TV (or TV stick / speaker) found on your Wi-Fi. */
data class CastDevice(
    /** Unique per device, e.g. a Cast route id or the DLNA UDN "uuid:…" */
    val id: String,
    val name: String,
    val kind: CastKind,
    /** Extra detail shown under the name, e.g. "Samsung QE65Q80". */
    val description: String? = null
)

/**
 * What the app tells the TV to play. [url] points at the phone's own little file server,
 * [artUrl] (if the song has album art) at the cover picture on the same server.
 */
data class CastMedia(
    val url: String,
    val title: String,
    val artist: String,
    val album: String,
    val mimeType: String,
    val sizeBytes: Long,
    val durationMs: Long,
    val artUrl: String? = null
)

enum class RemoteState { BUFFERING, PLAYING, PAUSED, IDLE, FINISHED, ERROR }

/** A snapshot of what the TV is doing right now. */
data class RemoteStatus(
    val state: RemoteState,
    val positionMs: Long,
    val durationMs: Long,
    /** 0..100, or null when the TV doesn't say. */
    val volume: Int? = null
)

/** Something went wrong while talking to the TV; [message] is shown to the user as-is. */
class CastException(message: String, cause: Throwable? = null) : Exception(message, cause)
