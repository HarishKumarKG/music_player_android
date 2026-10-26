package com.harish.mediaplayer.cast

import com.harish.mediaplayer.cast.dlna.DlnaRenderer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CastProtocolTest {

    @Test fun `range requests the TV sends`() {
        val size = 1_000L
        assertEquals(100L to 199L, CastServer.parseRange("bytes=100-199", size))
        assertEquals(100L to 999L, CastServer.parseRange("bytes=100-", size))       // "from 100 to the end"
        assertEquals(900L to 999L, CastServer.parseRange("bytes=-100", size))       // "the last 100 bytes"
        assertEquals(0L to 999L, CastServer.parseRange("bytes=0-5000", size))       // end past the file
        assertEquals(-1L to -1L, CastServer.parseRange(null, size))                 // no range: whole file
        assertEquals(-1L to -1L, CastServer.parseRange("items=1-2", size))
    }

    @Test fun `DLNA times`() {
        assertEquals("0:04:05", DlnaRenderer.formatTime(245_000))
        assertEquals("0:00:00", DlnaRenderer.formatTime(-5))
        assertEquals(245_000L, DlnaRenderer.parseTime("00:04:05"))
        assertEquals(245_000L, DlnaRenderer.parseTime("0:04:05.500"))
        assertEquals(0L, DlnaRenderer.parseTime("NOT_IMPLEMENTED"))
        assertEquals(0L, DlnaRenderer.parseTime(null))
    }

    @Test fun `reads values out of the TV's answers`() {
        val reply = """<s:Envelope><s:Body><u:GetPositionInfoResponse xmlns:u="urn:x">
            <Track>1</Track><TrackDuration>0:04:10</TrackDuration><RelTime>0:01:05</RelTime>
            </u:GetPositionInfoResponse></s:Body></s:Envelope>"""
        assertEquals("0:04:10", DlnaRenderer.tag(reply, "TrackDuration"))
        assertEquals("0:01:05", DlnaRenderer.tag(reply, "RelTime"))
        assertNull(DlnaRenderer.tag(reply, "AbsTime"))
        assertEquals("Tom & Jerry", DlnaRenderer.tag("<a:Title>Tom &amp; Jerry</a:Title>", "Title"))
    }

    @Test fun `song details for the TV are a music track with artist, album and cover`() {
        val didl = DlnaRenderer.didl(
            CastMedia(
                url = "http://192.168.1.5:4000/v/abc/Kannalane.flac",
                title = "Kannalane <Live>",
                artist = "A.R. Rahman & Chitra",
                album = "Bombay",
                mimeType = "audio/flac",
                sizeBytes = 123,
                durationMs = 61_000,
                artUrl = "http://192.168.1.5:4000/v/def/cover.jpg"
            )
        )
        assertTrue(didl.contains("<dc:title>Kannalane &lt;Live&gt;</dc:title>"))
        assertTrue(didl.contains("<upnp:artist>A.R. Rahman &amp; Chitra</upnp:artist>"))
        assertTrue(didl.contains("<upnp:album>Bombay</upnp:album>"))
        assertTrue(didl.contains("<upnp:albumArtURI>http://192.168.1.5:4000/v/def/cover.jpg</upnp:albumArtURI>"))
        assertTrue(didl.contains("object.item.audioItem.musicTrack"))
        assertTrue(didl.contains("http-get:*:audio/flac:DLNA.ORG_OP=01"))
        assertTrue(didl.contains("duration=\"0:01:01.000\""))
    }

    @Test fun `no cover - no albumArtURI`() {
        val didl = DlnaRenderer.didl(CastMedia("http://x/v/a/s.mp3", "S", "A", "B", "audio/mpeg", 1, 1_000))
        assertFalse(didl.contains("albumArtURI"))
    }

    @Test fun `file types the TV understands`() {
        assertEquals("audio/mpeg", audioMimeType("Song.MP3", "application/octet-stream"))
        assertEquals("audio/flac", audioMimeType("hi-res.flac", ""))
        assertEquals("audio/mp4", audioMimeType("track.m4a", ""))
        assertEquals("audio/x-ape", audioMimeType("old.ape", "audio/x-ape"))
        assertEquals("audio/mpeg", audioMimeType("noext", ""))
    }
}
