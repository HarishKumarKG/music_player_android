package com.harish.mediaplayer.domain.model

enum class ThemeMode(val label: String, val description: String) {
    SYSTEM("System default", "Follow the phone's light / dark setting"),
    LIGHT("Light", "Colourful pastel gradients"),
    DARK("Dark", "Deep night with glowing colours")
}
