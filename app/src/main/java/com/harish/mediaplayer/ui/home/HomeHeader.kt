package com.harish.mediaplayer.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.harish.mediaplayer.R
import com.harish.mediaplayer.domain.model.SortOrder
import com.harish.mediaplayer.ui.brand.GradientTitle
import com.harish.mediaplayer.ui.brand.HarishLogo
import com.harish.mediaplayer.ui.brand.themedBrandGradient
import kotlinx.coroutines.delay

@Composable
internal fun HomeHeader(
    onMenuClick: () -> Unit,
    isDark: Boolean,
    onToggleTheme: () -> Unit,
    isRefreshing: Boolean,
    onRefresh: (() -> Unit)?,  // null = hide refresh
    sort: HeaderSort?          // null = hide sort
) {
    val titleIn = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(500) // let the logo start first
        titleIn.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onMenuClick) {
            Icon(painterResource(R.drawable.baseline_menu_24), contentDescription = "Open menu")
        }
        Spacer(modifier = Modifier.width(4.dp))
        // Gradient badge, same look as the app icon
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(themedBrandGradient()))
        ) {
            HarishLogo(
                loop = false,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { scaleX = 1.3f; scaleY = 1.3f } // zoom so the H fills the badge
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .graphicsLayer {
                    alpha = titleIn.value
                    translationX = (1f - titleIn.value) * -16.dp.toPx() // slide in from the logo
                }
        ) {
            GradientTitle(text = "Harish")
            Text(
                text = "MUSIC PLAYER",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 3.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        ThemeToggleButton(isDark = isDark, onToggle = onToggleTheme)
        onRefresh?.let { RefreshButton(isRefreshing = isRefreshing, onClick = it) }
        sort?.let { SortMenu(it) }
    }
}

/**
 * Sun / moon toggle. Shows what you'll switch TO: a moon in light mode, a sun in dark mode.
 * The icon spins and pops as it swaps.
 */
@Composable
private fun ThemeToggleButton(isDark: Boolean, onToggle: () -> Unit) {
    IconButton(onClick = onToggle) {
        AnimatedContent(
            targetState = isDark,
            transitionSpec = {
                (fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.4f)) togetherWith
                    (fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.4f))
            },
            label = "themeToggle"
        ) { dark ->
            // Each new icon also does a quarter turn while it appears
            val turn = remember { Animatable(-90f) }
            LaunchedEffect(Unit) { turn.animateTo(0f, tween(400, easing = FastOutSlowInEasing)) }
            Icon(
                painter = painterResource(if (dark) R.drawable.baseline_light_mode_24 else R.drawable.baseline_dark_mode_24),
                contentDescription = if (dark) "Switch to light mode" else "Switch to dark mode",
                modifier = Modifier.graphicsLayer { rotationZ = turn.value }
            )
        }
    }
}

@Composable
private fun RefreshButton(isRefreshing: Boolean, onClick: () -> Unit) {
    // Swap the icon for a small spinner while MediaStore is being re-scanned
    IconButton(onClick = onClick, enabled = !isRefreshing) {
        if (isRefreshing) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            Icon(
                painter = painterResource(id = R.drawable.baseline_refresh_24),
                contentDescription = "Refresh songs"
            )
        }
    }
}

/** Sort choices shown in the header for whatever list is on screen. */
class HeaderSort(val options: List<String>, val selected: Int, val onSelect: (Int) -> Unit)

internal fun songHeaderSort(current: SortOrder, onSelect: (SortOrder) -> Unit) = HeaderSort(
    options = SortOrder.entries.map { it.label },
    selected = current.ordinal,
    onSelect = { onSelect(SortOrder.entries[it]) }
)

@Composable
private fun SortMenu(sort: HeaderSort) {
    var expanded by remember { mutableStateOf(false) }
    // Box anchors the dropdown to the icon so it opens from the top-right
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                painter = painterResource(id = R.drawable.baseline_sort_24),
                contentDescription = "Sort"
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            sort.options.forEachIndexed { index, label ->
                val isSelected = index == sort.selected
                DropdownMenuItem(
                    text = {
                        Text(
                            text = label,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.tertiary
                                    else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        sort.onSelect(index)
                        expanded = false
                    }
                )
            }
        }
    }
}
