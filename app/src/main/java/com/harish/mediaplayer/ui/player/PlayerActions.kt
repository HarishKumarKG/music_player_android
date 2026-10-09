package com.harish.mediaplayer.ui.player

import com.harish.mediaplayer.domain.model.Song

/** Everything the player UI can ask for, bundled so we don't pass 7 lambdas around. */
class PlayerActions(
    val playFromList: (songs: List<Song>, index: Int) -> Unit,
    val playQueueItem: (queueIndex: Int) -> Unit,
    val togglePlayPause: () -> Unit,
    val next: () -> Unit,
    val previous: () -> Unit,
    val seek: (positionMs: Long) -> Unit,
    val toggleShuffle: () -> Unit
)
