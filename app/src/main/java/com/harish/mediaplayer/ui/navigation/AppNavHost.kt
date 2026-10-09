package com.harish.mediaplayer.ui.navigation

import androidx.compose.material3.DrawerValue
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
            startDestination = startDestination
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
