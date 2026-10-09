package com.harish.mediaplayer.ui.home

import com.harish.mediaplayer.domain.model.Song

/** Everything the Home screen shows that comes from the ViewModel. */
data class HomeUiState(
    val songs: List<Song> = emptyList(),
    /** True until there is *something* to show (cache or MediaStore). */
    val isLoading: Boolean = true,
    /** True while MediaStore is being scanned. */
    val isRefreshing: Boolean = false
)
