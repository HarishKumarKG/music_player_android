package com.harish.mediaplayer.screen.main

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.provider.MediaStore
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.harish.mediaplayer.R
import com.harish.mediaplayer.util.MediaPlayerService

@Composable
fun MainScreenContent(
    viewModel: MainScreenViewModel = hiltViewModel(),
    context: Context
) {
    var songs by remember { mutableStateOf(listOf<String>()) }
    var currentPlayingIndex by remember { mutableStateOf(-1) } // -1 means no song is playing
    var isPlaying by remember { mutableStateOf(false) } // To track if a song is playing

    // Fetch songs when the screen is loaded
    LaunchedEffect(Unit) {
        songs = fetchSongs(context)
    }

    MainScreen(
        songs = songs,
        currentPlayingIndex = currentPlayingIndex,
        isPlaying = isPlaying,
        onPlayPauseClick = { index ->
            // Handle Play/Pause with the service
            val intent = Intent(context, MediaPlayerService::class.java)

            if (currentPlayingIndex == index && isPlaying) {
                // Pause the current song
                intent.putExtra("action", "pause")
                context.startService(intent)  // Start the service for pause
                isPlaying = false
            } else {
                // Play the selected song
                intent.putExtra("action", "play")
                intent.putExtra("song_path", songs[index])  // Pass the song path
                context.startService(intent)  // Start the service for play
                currentPlayingIndex = index
                isPlaying = true
            }
        }
    )
}

@Composable
private fun MainScreen(
    songs: List<String>,
    currentPlayingIndex: Int,
    isPlaying: Boolean,
    onPlayPauseClick: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Listing Local Songs",
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp),
            modifier = Modifier.padding(bottom = 20.dp)
        )

        // List songs using LazyColumn
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(songs.size) { index ->
                SongItem(
                    songName = songs[index],
                    isPlaying = currentPlayingIndex == index && isPlaying,
                    onPlayPauseClick = { onPlayPauseClick(index) }
                )
            }
        }
    }
}

@Composable
fun SongItem(
    songName: String,
    isPlaying: Boolean,
    onPlayPauseClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ),
        shape = MaterialTheme.shapes.medium,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = songName,
                style = TextStyle(
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onBackground
                ),
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onPlayPauseClick) {
                if (isPlaying) {
                    // Pause Icon
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_pause_24),
                        contentDescription = "Pause",
                        tint = Color.Black
                    )
                } else {
                    // Play Icon
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_play_arrow_24),
                        contentDescription = "Play",
                        tint = Color.Black
                    )
                }
            }
        }
    }
}

fun fetchSongs(context: Context): List<String> {
    val songList = mutableListOf<String>()
    val contentResolver = context.contentResolver
    val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

    val projection = arrayOf(
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.DATA // This is the file path
    )

    val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
    val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

    val cursor = contentResolver.query(uri, projection, selection, null, sortOrder)

    cursor?.use {
        val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val dataColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA) // Get the file path

        while (it.moveToNext()) {
            val title = it.getString(titleColumn)
            val data = it.getString(dataColumn) // Get the file path
            songList.add(data) // Add file path to the list
        }
    }
    return songList
}