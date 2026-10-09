package com.harish.mediaplayer.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.harish.mediaplayer.R
import com.harish.mediaplayer.domain.library.SongSearchIndex
import com.harish.mediaplayer.domain.model.Song
import com.harish.mediaplayer.domain.model.SortOrder
import com.harish.mediaplayer.domain.model.sortedByOrder
import com.harish.mediaplayer.ui.common.songCount
import com.harish.mediaplayer.ui.library.ListDivider
import com.harish.mediaplayer.ui.library.SongListHandlers
import com.harish.mediaplayer.ui.library.SongRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Rounded "Search songs, artists, albums" bar under the header. Tapping it opens search. */
@Composable
internal fun SearchEntry(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painterResource(R.drawable.baseline_search_24),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(12.dp))
        Text(
            "Search songs, artists, albums",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** How long typing must pause before the results update. */
private const val SEARCH_DEBOUNCE_MS = 200L

/**
 * Search mode header: ← back, the text field (keyboard opens straight away) and ✕ clear.
 *
 * The typed text lives HERE, so each keystroke only redraws this small field. The rest of the
 * screen hears about it ([onQueryChange]) once typing pauses for [SEARCH_DEBOUNCE_MS].
 */
@Composable
internal fun SearchTopBar(initialQuery: String, onQueryChange: (String) -> Unit, onClose: () -> Unit) {
    var text by rememberSaveable { mutableStateOf(initialQuery) }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    // Debounce: every keystroke restarts this, so only the text you stop at is sent on
    LaunchedEffect(text) {
        if (text.isNotEmpty()) delay(SEARCH_DEBOUNCE_MS)
        onQueryChange(text)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onClose) {
            Icon(painterResource(R.drawable.baseline_arrow_back_24), contentDescription = "Close search")
        }
        TextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text("Search songs, artists, albums") },
            singleLine = true,
            shape = RoundedCornerShape(50),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                onQueryChange(text) // Search key: don't wait for the pause
                keyboard?.hide()
            }),
            trailingIcon = {
                if (text.isNotEmpty()) {
                    IconButton(onClick = { text = "" }) {
                        Icon(painterResource(R.drawable.baseline_close_24), contentDescription = "Clear search")
                    }
                }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.tertiary
            ),
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
                .focusRequester(focusRequester)
        )
    }
}

/**
 * Results for [query], in the chosen sort order. Playing a result makes the results the queue,
 * so next/previous move through what you searched for.
 *
 * The [index] is prepared ahead of time by the ViewModel, so searching is just quick text
 * checks, done in the background. A progress line shows only while the index is still being
 * prepared (e.g. right after the app starts).
 */
@Composable
internal fun SearchResults(
    query: String,
    index: SongSearchIndex?,
    sortOrder: SortOrder,
    handlers: SongListHandlers,
    contentPadding: PaddingValues
) {
    var results by remember { mutableStateOf<List<Song>>(emptyList()) }
    LaunchedEffect(query, index, sortOrder) {
        if (index == null) return@LaunchedEffect
        results = if (query.isBlank()) emptyList()
                  else withContext(Dispatchers.Default) { index.search(query).sortedByOrder(sortOrder) }
    }
    val preparing = index == null && query.isNotBlank()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding() // last results stay above the keyboard
    ) {
        when {
            query.isBlank() -> Hint("Type a song, artist, album or file name")
            preparing -> LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                color = MaterialTheme.colorScheme.tertiary
            )
            results.isEmpty() -> Hint("No songs match \"${query.trim()}\"")
            else -> LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = contentPadding) {
                item {
                    Text(
                        songCount(results.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
                items(results, key = { it.id }, contentType = { "song" }) { song ->
                    SongRow(song = song, list = results, handlers = handlers, subtitle = "${song.artist} · ${song.album}")
                    ListDivider()
                }
            }
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(32.dp)
    )
}
