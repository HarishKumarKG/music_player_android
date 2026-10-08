package com.harish.mediaplayer.navigation

sealed class Screen(val route: String) {
    object Main : Screen(route = "main")
    object Theme : Screen(route = "theme")
}
