package com.harish.mediaplayer.ui.themepicker

import androidx.lifecycle.ViewModel
import com.harish.mediaplayer.data.settings.ThemeRepository
import com.harish.mediaplayer.domain.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val themeRepository: ThemeRepository
) : ViewModel() {
    val themeMode: StateFlow<ThemeMode> = themeRepository.themeMode
    fun onThemeSelected(mode: ThemeMode) = themeRepository.setThemeMode(mode)
}
