package com.harish.mediaplayer.screen.theme

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.harish.mediaplayer.R
import com.harish.mediaplayer.settings.ThemeMode
import com.harish.mediaplayer.ui.brand.themedBrandGradient
import com.harish.mediaplayer.ui.brand.BrandGradient
import com.harish.mediaplayer.ui.brand.DarkBrandGradient
import com.harish.mediaplayer.ui.brand.GradientTitle
import com.harish.mediaplayer.ui.theme.BrandBackground
import com.harish.mediaplayer.ui.theme.LocalIsDarkTheme
import com.harish.mediaplayer.ui.theme.drawDarkBackdrop
import com.harish.mediaplayer.ui.theme.drawLightBackdrop

@Composable
fun ThemeScreen(
    onMenuClick: () -> Unit,
    viewModel: ThemeViewModel = hiltViewModel()
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val isDarkNow = LocalIsDarkTheme.current

    BrandBackground(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
        ) {
            // Header: burger + gradient title
            Row(
                modifier = Modifier.padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onMenuClick) {
                    Icon(painterResource(R.drawable.baseline_menu_24), contentDescription = "Open menu")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    GradientTitle(text = "Theme")
                    Text(
                        text = "MAKE HARISH YOURS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 3.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Three phone-shaped preview cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ThemeMode.entries.forEach { mode ->
                    ThemePreviewCard(
                        mode = mode,
                        selected = mode == themeMode,
                        onClick = { viewModel.onThemeSelected(mode) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Frosted info card describing the current choice (cross-fades on change)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                tonalElevation = 2.dp
            ) {
                AnimatedContent(
                    targetState = themeMode,
                    transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                    label = "themeInfo"
                ) { mode ->
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(themedBrandGradient())),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.baseline_palette_24),
                                contentDescription = null,
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = mode.label,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = mode.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (mode == ThemeMode.SYSTEM) {
                                Text(
                                    text = "Right now: ${if (isDarkNow) "Dark" else "Light"}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** A mini phone showing what the app looks like in that theme. */
@Composable
private fun ThemePreviewCard(
    mode: ThemeMode,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Selected card pops forward with a little spring; others sit back slightly
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.92f,
        animationSpec = spring(dampingRatio = 0.55f),
        label = "cardScale"
    )
    val shape = RoundedCornerShape(20.dp)

    Column(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(24.dp))
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.55f) // phone shape
                .border(
                    width = if (selected) 3.dp else 1.dp,
                    brush = if (selected) Brush.linearGradient(themedBrandGradient())
                            else SolidColor(MaterialTheme.colorScheme.outlineVariant),
                    shape = shape
                )
                .padding(if (selected) 3.dp else 1.dp)
                .clip(shape)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                when (mode) {
                    ThemeMode.LIGHT -> drawMiniPhone(dark = false)
                    ThemeMode.DARK -> drawMiniPhone(dark = true)
                    ThemeMode.SYSTEM -> {
                        // Light on the top-left half, dark on the bottom-right half
                        drawMiniPhone(dark = false)
                        val diagonal = Path().apply {
                            moveTo(size.width, 0f)
                            lineTo(size.width, size.height)
                            lineTo(0f, size.height)
                            close()
                        }
                        clipPath(diagonal) { drawMiniPhone(dark = true) }
                    }
                }
            }
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(themedBrandGradient())),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.baseline_check_24),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = mode.label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

/** Tiny drawing of the songs screen: header, song rows and the mini player. */
private fun DrawScope.drawMiniPhone(dark: Boolean) {
    if (dark) drawDarkBackdrop() else drawLightBackdrop()
    val w = size.width
    val h = size.height
    val line = if (dark) Color.White.copy(alpha = 0.22f) else Color(0xFF1D3557).copy(alpha = 0.16f)
    val thumb = if (dark) Color.White.copy(alpha = 0.14f) else Color(0xFF1D3557).copy(alpha = 0.10f)
    val player = if (dark) Color(0xFF2C2C30) else Color(0xFFA8DADC)
    val accent = Color(0xFFE63946) // Strawberry Red play button in both
    val r = CornerRadius(w * 0.03f)

    // Header: gradient logo dot + title line
    drawCircle(
        brush = Brush.linearGradient(if (dark) DarkBrandGradient else BrandGradient),
        radius = w * 0.07f,
        center = Offset(w * 0.15f, h * 0.08f)
    )
    drawRoundRect(accent.copy(alpha = 0.8f), Offset(w * 0.27f, h * 0.065f), Size(w * 0.36f, h * 0.028f), r)

    // Song rows
    for (i in 0 until 5) {
        val y = h * (0.17f + i * 0.115f)
        drawRoundRect(thumb, Offset(w * 0.08f, y), Size(w * 0.16f, w * 0.16f), CornerRadius(w * 0.04f))
        drawRoundRect(
            line,
            Offset(w * 0.30f, y + w * 0.06f),
            Size(w * (0.58f - (i % 2) * 0.16f), h * 0.022f),
            r
        )
    }

    // Mini player pill with a play button
    drawRoundRect(player, Offset(w * 0.06f, h * 0.80f), Size(w * 0.88f, h * 0.12f), CornerRadius(w * 0.08f))
    drawRoundRect(thumb, Offset(w * 0.11f, h * 0.825f), Size(w * 0.14f, w * 0.14f), CornerRadius(w * 0.03f))
    drawRoundRect(line, Offset(w * 0.30f, h * 0.85f), Size(w * 0.3f, h * 0.02f), r)
    drawCircle(accent, radius = w * 0.065f, center = Offset(w * 0.80f, h * 0.86f))
}
