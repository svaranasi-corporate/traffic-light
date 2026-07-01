package com.trafficlight

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.trafficlight.ui.menu.MenuScreen
import com.trafficlight.ui.settings.SettingsScreen
import com.trafficlight.ui.theme.TrafficLightTheme
import com.trafficlight.ui.trafficlight.TrafficLightScreen

/** Route constants for type-safe navigation. */
private object Routes {
    const val MENU = "menu"
    const val TRAFFIC_LIGHT = "trafficlight"
    const val SETTINGS = "settings"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TrafficLightTheme {
                TrafficLightApp()
            }
        }
    }
}

@Composable
private fun TrafficLightApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.MENU,
    ) {
        composable(Routes.MENU) {
            MenuScreen(
                onStartClick = {
                    // Avoid duplicate backstack entries on rapid taps
                    navController.navigate(Routes.TRAFFIC_LIGHT) {
                        launchSingleTop = true
                    }
                },
                onOptionsClick = {
                    navController.navigate(Routes.SETTINGS) {
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(Routes.TRAFFIC_LIGHT) {
            TrafficLightScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
            )
        }
    }
}
