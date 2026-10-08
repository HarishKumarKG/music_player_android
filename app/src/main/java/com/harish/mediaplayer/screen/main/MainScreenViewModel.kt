package com.harish.mediaplayer.screen.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harish.mediaplayer.screen.main.repository.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainScreenViewModel @Inject constructor(
    private val songRepository: SongRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MainScreenState.initValue)
    val state: StateFlow<MainScreenState> = _state.asStateFlow()

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
            refresh(userTriggered = false)
        }
    }

    fun onEvent(event: MainViewEvent) {
        when (event) {
            is OnRefreshEvent -> viewModelScope.launch { refresh(userTriggered = true) }
        }
    }

    private suspend fun refresh(userTriggered: Boolean) {
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
