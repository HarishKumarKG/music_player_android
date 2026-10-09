package com.harish.mediaplayer.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harish.mediaplayer.data.library.LibraryRepository
import com.harish.mediaplayer.data.song.SongRepository
import com.harish.mediaplayer.domain.model.LibraryData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val songRepository: SongRepository,
    private val libraryRepository: LibraryRepository
) : ViewModel() {

    /** Favourites + playlists (saved on disk by LibraryRepository). */
    val library: StateFlow<LibraryData> = libraryRepository.data

    fun toggleFavorite(songId: Long) = libraryRepository.toggleFavorite(songId)
    fun createPlaylist(name: String, firstSongId: Long? = null) = libraryRepository.createPlaylist(name, firstSongId)
    fun renamePlaylist(id: Long, name: String) = libraryRepository.renamePlaylist(id, name)
    fun deletePlaylist(id: Long) = libraryRepository.deletePlaylist(id)
    fun addToPlaylist(id: Long, songId: Long) = libraryRepository.addToPlaylist(id, songId)
    fun removeFromPlaylist(id: Long, songId: Long) = libraryRepository.removeFromPlaylist(id, songId)
    fun reorderPlaylist(id: Long, orderedIds: List<Long>) = libraryRepository.reorderPlaylist(id, orderedIds)

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    // One-time UI messages (shown as a Toast)
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    init {
        viewModelScope.launch {
            // 1. Show the cached list straight away
            val cached = songRepository.loadCached()
            if (cached.isNotEmpty()) {
                _state.update { it.copy(songs = cached, isLoading = false) }
            }
            // 2. Then quietly re-scan MediaStore and swap in the fresh list
            loadSongs(userTriggered = false)
        }
    }

    /** Refresh button: re-scan MediaStore and report what changed. */
    fun refresh() {
        viewModelScope.launch { loadSongs(userTriggered = true) }
    }

    private suspend fun loadSongs(userTriggered: Boolean) {
        if (_state.value.isRefreshing) return
        _state.update { it.copy(isRefreshing = true) }

        val before = _state.value.songs
        val fresh = try {
            songRepository.refresh()
        } catch (e: Exception) {
            _state.update { it.copy(isLoading = false, isRefreshing = false) }
            if (userTriggered) _messages.tryEmit("Couldn't load songs")
            return
        }

        _state.update { it.copy(songs = fresh, isLoading = false, isRefreshing = false) }

        if (userTriggered) {
            val oldIds = before.mapTo(HashSet()) { it.id }
            val added = fresh.count { it.id !in oldIds }
            _messages.tryEmit(
                when {
                    added == 1 -> "1 new song found"
                    added > 1 -> "$added new songs found"
                    else -> "Song list is up to date"
                }
            )
        }
    }
}
