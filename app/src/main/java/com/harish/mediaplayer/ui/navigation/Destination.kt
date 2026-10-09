package com.harish.mediaplayer.ui.navigation

/** Top-level screens reachable from the burger menu. */
sealed class Destination(val route: String) {
    data object Home : Destination(route = "home")
    data object Theme : Destination(route = "theme")
}
