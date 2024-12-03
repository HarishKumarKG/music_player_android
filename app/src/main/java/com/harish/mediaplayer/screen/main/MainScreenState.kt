package com.harish.mediaplayer.screen.main

data class MainScreenState(
    val counter: Int
) {
    companion object {
        val initValue = MainScreenState(counter = 0)
    }
}