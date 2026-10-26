package com.harish.mediaplayer.ui.navigation

import androidx.compose.material3.DrawerValue
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.harish.mediaplayer.ui.player.SleepTimerSheetHost
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.harish.mediaplayer.ui.home.HomeRoute
import com.harish.mediaplayer.ui.themepicker.ThemeScreen
import kotlinx.coroutines.launch

/**
 * One burger-menu drawer shared by every screen. Screens only get `onMenuClick`
 * to open it; the drawer decides where to navigate.
 */
@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Destination.Home.route,
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val openDrawer: () -> Unit = { scope.launch { drawerState.open() } }
    var showSleepTimer by rememberSaveable { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        // Swipe only CLOSES the open menu. A closed menu opens with the ☰ button, so a swipe from
        // the left edge is Android's "back" gesture again instead of pulling the menu out.
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            AppDrawer(
                currentRoute = currentRoute,
                // true while opening/open, so the profile slideshow only runs when visible
                isOpen = drawerState.targetValue == DrawerValue.Open,
                onSleepTimerClick = {
                    scope.launch { drawerState.close() }
                    showSleepTimer = true
                },
                onDestinationClick = { destination ->
                    scope.launch { drawerState.close() }
                    if (destination.route != currentRoute) {
                        navController.navigate(destination.route) {
                            // Songs stays at the bottom of the stack, so Back always returns there
                            popUpTo(Destination.Home.route)
                            launchSingleTop = true
                        }
                    }
                }
            )
        }
    ) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            // Short, light animations. The default is a slow 0.7 s cross-fade, which made going
            // back (especially the back swipe) feel laggy.
            enterTransition = { fadeIn(tween(220)) + slideInHorizontally(tween(220)) { it / 12 } },
            exitTransition = { fadeOut(tween(150)) },
            popEnterTransition = { fadeIn(tween(220)) },
            popExitTransition = { fadeOut(tween(180)) + slideOutHorizontally(tween(220)) { it / 12 } }
        ) {
            composable(route = Destination.Home.route) {
                HomeRoute(onMenuClick = openDrawer)
            }
            composable(route = Destination.Theme.route) {
                ThemeScreen(onMenuClick = openDrawer)
            }
        }
    }

    if (showSleepTimer) {
        SleepTimerSheetHost(onDismiss = { showSleepTimer = false })
    }
}
