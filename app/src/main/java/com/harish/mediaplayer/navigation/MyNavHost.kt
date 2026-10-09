package com.harish.mediaplayer.navigation

import android.content.Context
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.harish.mediaplayer.screen.main.MainScreenContent
import com.harish.mediaplayer.screen.theme.ThemeScreen
import kotlinx.coroutines.launch

/**
 * One burger-menu drawer shared by every screen. Screens only get `onMenuClick`
 * to open it; the drawer decides where to navigate.
 */
@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Main.route,
) {
    val context: Context = LocalContext.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val openDrawer: () -> Unit = { scope.launch { drawerState.open() } }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(
                currentRoute = currentRoute,
                // true while opening/open, so the profile slideshow only runs when visible
                isOpen = drawerState.targetValue == DrawerValue.Open,
                onDestinationClick = { screen ->
                    scope.launch { drawerState.close() }
                    if (screen.route != currentRoute) {
                        navController.navigate(screen.route) {
                            // Songs stays at the bottom of the stack, so Back always returns there
                            popUpTo(Screen.Main.route)
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
            composable(route = Screen.Main.route) {
                MainScreenContent(context = context, onMenuClick = openDrawer)
            }
            composable(route = Screen.Theme.route) {
                ThemeScreen(onMenuClick = openDrawer)
            }
        }
    }
}
