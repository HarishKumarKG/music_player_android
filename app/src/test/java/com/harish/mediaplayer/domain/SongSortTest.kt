package com.harish.mediaplayer.domain

import com.harish.mediaplayer.domain.model.SortOrder
import com.harish.mediaplayer.domain.model.sortedByOrder
import com.harish.mediaplayer.song
import org.junit.Assert.assertEquals
import org.junit.Test

class SongSortTest {

    private val songs = listOf(
        song(1, title = "banana", dateModifiedSec = 10),
        song(2, title = "Apple", dateModifiedSec = 30),
        song(3, title = "cherry", dateModifiedSec = 20)
    )

    @Test
    fun `date modified puts the newest first`() {
        assertEquals(listOf(2L, 3L, 1L), songs.sortedByOrder(SortOrder.DATE_MODIFIED).map { it.id })
    }

    @Test
    fun `name A-Z ignores upper and lower case`() {
        assertEquals(listOf("Apple", "banana", "cherry"), songs.sortedByOrder(SortOrder.NAME_ASC).map { it.title })
    }

    @Test
    fun `name Z-A is the reverse`() {
        assertEquals(listOf("cherry", "banana", "Apple"), songs.sortedByOrder(SortOrder.NAME_DESC).map { it.title })
    }
}
