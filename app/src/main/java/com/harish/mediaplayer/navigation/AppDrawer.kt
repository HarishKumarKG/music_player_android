package com.harish.mediaplayer.navigation

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.harish.mediaplayer.R
import com.harish.mediaplayer.ui.brand.BrandGradient
import com.harish.mediaplayer.ui.brand.HarishLogo

/** Profile shown at the top of the menu — edit these to change what's displayed. */
private object Profile {
    const val NAME = "Harish"
    const val ROLE = "Mobile App Developer"
    const val LOCATION = "Chennai, India"
    const val GITHUB = "github.com/HarishKumarKG"
    val SKILLS = listOf("Android", "iOS", "Flutter", "KMP", ".NET MAUI")
}

/** One entry in the burger menu. Add a line here to add a new screen to the menu. */
private data class DrawerDestination(val screen: Screen, val label: String, @DrawableRes val icon: Int)

private val drawerDestinations = listOf(
    DrawerDestination(Screen.Main, "Songs", R.drawable.baseline_music_note_24),
    DrawerDestination(Screen.Theme, "Theme", R.drawable.baseline_palette_24)
)

@Composable
fun AppDrawer(currentRoute: String?, onDestinationClick: (Screen) -> Unit) {
    ModalDrawerSheet {
        ProfileHeader()

        Spacer(modifier = Modifier.height(8.dp))
        drawerDestinations.forEach { item ->
            NavigationDrawerItem(
                label = { Text(item.label) },
                icon = { Icon(painterResource(item.icon), contentDescription = null) },
                selected = currentRoute == item.screen.route,
                onClick = { onDestinationClick(item.screen) },
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

/** Gradient card: profile photo with the H badge, name, role, skills and links. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(BrandGradient))
            .padding(vertical = 24.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Round photo with a white ring and a small H logo badge
        Box {
            Image(
                painter = painterResource(R.drawable.profile_harish),
                contentDescription = "Photo of ${Profile.NAME}",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(104.dp)
                    .shadow(12.dp, CircleShape)
                    .clip(CircleShape)
                    .border(3.dp, Color.White, CircleShape)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(BrandGradient))
            ) {
                HarishLogo(loop = false, modifier = Modifier.size(30.dp))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(Profile.NAME, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
        Text(
            Profile.ROLE.uppercase(),
            color = Color.White.copy(alpha = 0.9f),
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
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.2f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(Profile.LOCATION, color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodySmall)
        Text(Profile.GITHUB, color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodySmall)
    }
}
