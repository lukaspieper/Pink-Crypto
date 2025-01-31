/*
 * SPDX-FileCopyrightText: 2022 Lukas Pieper
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package de.lukaspieper.truvark

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import dagger.hilt.android.AndroidEntryPoint
import de.lukaspieper.truvark.ui.theme.AppTheme
import de.lukaspieper.truvark.ui.views.browser.BrowserPage
import de.lukaspieper.truvark.ui.views.launcher.LauncherPage
import de.lukaspieper.truvark.ui.views.presenter.PresenterPage
import de.lukaspieper.truvark.ui.views.settings.SettingsHomePage

/**
 * This activity is the only one in this app.
 */
@AndroidEntryPoint
class Activity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Calling before onCreate because of this exception: https://stackoverflow.com/a/73129726
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = Page.Launcher,
                    modifier = Modifier.background(MaterialTheme.colorScheme.background)
                ) {
                    composable<Page.Launcher> {
                        LauncherPage(
                            navigateAndClearBackStack = { route ->
                                navController.navigateSafely(route) {
                                    popUpTo(0)
                                }
                            }
                        )
                    }
                    composable<Page.Browser> {
                        BrowserPage(navigate = { route -> navController.navigateSafely(route) })
                    }
                    composable<Page.Presenter> { navigation ->
                        val route: Page.Presenter = navigation.toRoute()

                        PresenterPage(
                            parameters = route,
                            navigateBack = navController::popBackStack
                        )
                    }
                    composable<Page.SettingsHome> {
                        SettingsHomePage(
                            navigateBack = navController::popBackStack
                        )
                    }
                }
            }
        }
    }

    private fun NavController.navigateSafely(route: Page, builder: NavOptionsBuilder.() -> Unit = {}) {
        // This approach prevents multi-touch navigation, e.g. touching multiple files in the file browser would
        // lead to multiple navigation events. Navigating back would need to go through all of them.
        if (currentDestination?.hasRoute(route::class) == false) {
            navigate(route, builder)
        }
    }
}
