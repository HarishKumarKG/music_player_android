package com.harish.mediaplayer.ui.navigation

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.harish.mediaplayer.R
import com.harish.mediaplayer.ui.brand.BrandDeepSpace
import com.harish.mediaplayer.ui.brand.BrandFrosted
import com.harish.mediaplayer.ui.brand.BrandHoneydew
import com.harish.mediaplayer.ui.brand.BrandRed
import com.harish.mediaplayer.ui.brand.BrandSteel
import com.harish.mediaplayer.ui.brand.DarkCharcoal
import com.harish.mediaplayer.ui.brand.DarkGraphite
import com.harish.mediaplayer.ui.brand.DarkGunmetal
import com.harish.mediaplayer.ui.brand.HarishLogo
import com.harish.mediaplayer.ui.brand.themedBrandGradient
import com.harish.mediaplayer.ui.theme.LocalIsDarkTheme
import kotlinx.coroutines.delay

/** Profile shown at the top of the menu — edit these to change what's displayed. */
private object Profile {
    const val NAME = "Harish"
    const val ROLE = "Mobile App Developer"
    const val LOCATION = "Chennai, India"
    const val GITHUB = "github.com/HarishKumarKG"
    val SKILLS = listOf("Android", "iOS", "Flutter", "KMP", ".NET MAUI")
}

/** One entry in the burger menu. Add a line here to add a new screen to the menu. */
private data class DrawerDestination(val destination: Destination, val label: String, @DrawableRes val icon: Int)

/** Profile photos shown in the menu; one is picked at random and they keep changing. */
private val profilePhotos = listOf(
    R.drawable.profile_harish_1,
    R.drawable.profile_harish_2,
    R.drawable.profile_harish_3,
    R.drawable.profile_harish_4,
    R.drawable.profile_harish_5,
    R.drawable.profile_harish_6,
    R.drawable.profile_harish_7
)

private val drawerDestinations = listOf(
    DrawerDestination(Destination.Home, "Songs", R.drawable.baseline_music_note_24),
    DrawerDestination(Destination.Theme, "Theme", R.drawable.baseline_palette_24)
)

@Composable
fun AppDrawer(currentRoute: String?, isOpen: Boolean, onDestinationClick: (Destination) -> Unit) {
    ModalDrawerSheet {
        ProfileHeader(isOpen = isOpen)

        Spacer(modifier = Modifier.height(8.dp))
        drawerDestinations.forEach { item ->
            NavigationDrawerItem(
                label = { Text(item.label) },
                icon = { Icon(painterResource(item.icon), contentDescription = null) },
                selected = currentRoute == item.destination.route,
                onClick = { onDestinationClick(item.destination) },
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))
        HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp))
        Text(
            text = "Harish Music Player",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)
        )
    }
}

/**
 * Profile card that follows the theme:
 *  Light: Frosted Blue -> Honeydew card with Deep Space text.
 *  Dark:  Deep Space -> Steel Blue card with Honeydew text.
 * Animation: a gradient ring keeps rotating around the photo (and the photos change, see below).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileHeader(isOpen: Boolean) {
    val dark = LocalIsDarkTheme.current
    val cardColors = if (dark) listOf(DarkCharcoal, DarkGraphite) else listOf(BrandFrosted, BrandHoneydew)
    val textColor = if (dark) Color(0xFFF1F1F1) else BrandDeepSpace
    val chipBackground = if (dark) Color.White.copy(alpha = 0.15f) else BrandDeepSpace.copy(alpha = 0.08f)
    // Rotating ring: silver/graphite with one red streak in dark, the full palette in light
    val ringColors = if (dark) listOf(Color.White, DarkGunmetal, Color(0xFF8E8F96), DarkGunmetal, BrandRed, Color.White)
                     else listOf(BrandRed, BrandFrosted, BrandSteel, BrandHoneydew, BrandRed)

    val infinite = rememberInfiniteTransition(label = "profile")
    val ringAngle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing)),
        label = "ring"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(cardColors))
            .padding(vertical = 24.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Photo inside a rotating gradient ring, with a small H logo badge
        Box {
            Box(
                modifier = Modifier
                    .size(116.dp)
                    .drawBehind {
                        rotate(ringAngle) {
                            drawCircle(
                                brush = Brush.sweepGradient(ringColors),
                                radius = size.minDimension / 2 - 2.dp.toPx(),
                                style = Stroke(width = 4.dp.toPx())
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                ProfilePhotoSlideshow(
                    isOpen = isOpen,
                    modifier = Modifier
                        .size(102.dp)
                        .shadow(10.dp, CircleShape)
                        .clip(CircleShape)
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 4.dp, bottom = 4.dp)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(themedBrandGradient()))
            ) {
                HarishLogo(loop = false, modifier = Modifier.size(30.dp))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(Profile.NAME, color = textColor, fontSize = 28.sp, fontWeight = FontWeight.Black)
        Text(
            Profile.ROLE.uppercase(),
            color = textColor.copy(alpha = 0.85f),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(12.dp))
        // Skill chips wrap onto a second line when needed
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Profile.SKILLS.forEach { skill ->
                Text(
                    text = skill,
                    color = textColor,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(chipBackground)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(Profile.LOCATION, color = textColor.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
        Text(Profile.GITHUB, color = textColor.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
    }
}

/**
 * Random photo "magic":
 *  - opens on a random photo, then every 3.5s jumps to another random one
 *  - the change is a zoom-crossfade: new photo grows in from 80%, old one zooms out to 120% and fades
 *  - while a photo is showing it slowly drifts closer (Ken Burns effect)
 *  - tap the photo to shuffle right away
 * The timer only runs while the menu is open, so it costs nothing when closed.
 */
@Composable
private fun ProfilePhotoSlideshow(isOpen: Boolean, modifier: Modifier = Modifier) {
    var current by rememberSaveable { mutableIntStateOf(profilePhotos.indices.random()) }
    fun showRandomOther() {
        current = (profilePhotos.indices - current).random() // never the same photo twice in a row
    }

    LaunchedEffect(isOpen) {
        while (isOpen) {
            delay(3500)
            showRandomOther()
        }
    }

    AnimatedContent(
        targetState = current,
        transitionSpec = {
            (fadeIn(tween(700)) + scaleIn(tween(700), initialScale = 0.8f)) togetherWith
                (fadeOut(tween(700)) + scaleOut(tween(700), targetScale = 1.2f))
        },
        label = "profilePhoto",
        modifier = modifier.clickable { showRandomOther() }
    ) { index ->
        // Ken Burns: each photo slowly zooms in while it's on screen
        val drift = remember { Animatable(1f) }
        LaunchedEffect(Unit) { drift.animateTo(1.12f, tween(4200, easing = LinearEasing)) }
        Image(
            painter = painterResource(profilePhotos[index]),
            contentDescription = "Photo of ${Profile.NAME}",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = drift.value
                    scaleY = drift.value
                }
        )
    }
}
