package com.harish.mediaplayer.domain

import com.harish.mediaplayer.domain.rename.NameCheck
import com.harish.mediaplayer.domain.rename.baseNameOf
import com.harish.mediaplayer.domain.rename.checkNewFileName
import com.harish.mediaplayer.domain.rename.checkNewFolderName
import com.harish.mediaplayer.domain.rename.extensionOf
import com.harish.mediaplayer.domain.rename.namesInside
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NewFileNameTest {

    private fun check(typed: String, current: String = "Trip.mp4", others: List<String> = emptyList()) =
        checkNewFileName(typed, current, others)

    @Test fun `splits name and extension`() {
        assertEquals("Trip to Ooty", baseNameOf("Trip to Ooty.mp4"))
        assertEquals("mp4", extensionOf("Trip to Ooty.mp4"))
        assertEquals("README", baseNameOf("README"))
        assertEquals("", extensionOf("README"))
        assertEquals("archive.tar", baseNameOf("archive.tar.gz"))
    }

    @Test fun `keeps the extension for you`() {
        assertEquals(NameCheck.Ok("Ooty 2024.mp4"), check("Ooty 2024"))
        assertEquals(NameCheck.Ok("Ooty 2024.mp4"), check("  Ooty 2024  "))
        assertEquals(NameCheck.Ok("Ooty.mp4"), check("Ooty.mp4")) // typed it too: not doubled
    }

    @Test fun `blank or unchanged keeps the button off without a message`() {
        assertEquals(NameCheck.Problem(""), check(""))
        assertEquals(NameCheck.Problem(""), check("Trip"))
    }

    @Test fun `bad characters and dots are refused`() {
        assertTrue(check("a/b") is NameCheck.Problem)
        assertTrue(check("what?") is NameCheck.Problem)
        assertTrue(check(".hidden") is NameCheck.Problem)
        assertTrue(check("name.") is NameCheck.Problem)
    }

    @Test fun `clash with another file is refused, ignoring case`() {
        val result = check("ooty", others = listOf("Ooty.MP4"))
        assertTrue(result is NameCheck.Problem && result.reason.isNotEmpty())
    }

    @Test fun `changing only the case is allowed`() {
        assertEquals(NameCheck.Ok("trip.mp4"), check("trip"))
    }

    @Test fun `very long names are refused`() {
        assertTrue(check("x".repeat(300)) is NameCheck.Problem)
    }

    @Test fun `folder names keep dots and have no extension added`() {
        assertEquals(NameCheck.Ok("Ooty 2024"), checkNewFolderName("Ooty 2024", "Trip", emptyList()))
        assertEquals(NameCheck.Ok("Season 1.5"), checkNewFolderName("Season 1.5", "Trip", emptyList()))
        assertEquals(NameCheck.Problem(""), checkNewFolderName("Trip", "Trip", emptyList()))
        assertTrue(checkNewFolderName("Songs", "Trip", listOf("songs", "Trip")) is NameCheck.Problem)
    }

    @Test fun `names inside a folder come from the file paths`() {
        val paths = listOf("/s/Movies/Ooty/a.mp4", "/s/Movies/Ooty/b.mp4", "/s/Movies/c.mp4", "/s/Music/d.mp3")
        assertEquals(setOf("Ooty", "c.mp4"), namesInside("/s/Movies", paths))
    }
}
