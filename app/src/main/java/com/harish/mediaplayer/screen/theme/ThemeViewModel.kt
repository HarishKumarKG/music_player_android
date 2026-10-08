package com.harish.mediaplayer.screen.theme

import androidx.lifecycle.ViewModel
import com.harish.mediaplayer.settings.ThemeMode
import com.harish.mediaplayer.settings.ThemeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val themeRepository: ThemeRepository
) : ViewModel() {
    val themeMode: StateFlow<ThemeMode> = themeRepository.themeMode
    fun onThemeSelected(mode: ThemeMode) = themeRepository.setThemeMode(mode)
}
