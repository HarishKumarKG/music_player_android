package com.harish.mediaplayer.screen.main

import com.harish.mediaplayer.screen.main.model.Song

data class MainScreenState(
    val songs: List<Song>,
    val isLoading: Boolean,    // true until we have *something* to show (cache or MediaStore)
    val isRefreshing: Boolean  // true while MediaStore is being scanned
) {
    companion object {
        val initValue = MainScreenState(songs = emptyList(), isLoading = true, isRefreshing = false)
    }
}
