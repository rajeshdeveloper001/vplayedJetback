package com.vplayed.feature.splashscreen.presentation.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.vplayed.feature.splashscreen.presentation.screen.SplashRoute
import kotlinx.serialization.Serializable

/** Start destination. Removed from the back stack once the app has launched. */
@Serializable
data object SplashDestination

fun NavGraphBuilder.splashScreen(onSplashFinished: () -> Unit) {
    composable<SplashDestination> { SplashRoute(onSplashFinished = onSplashFinished) }
}
