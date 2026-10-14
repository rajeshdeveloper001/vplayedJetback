package com.vplayed.feature.settings.presentation.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.vplayed.feature.settings.presentation.screen.AppearanceSettingsRoute
import kotlinx.serialization.Serializable

@Serializable
data object AppearanceDestination

fun NavGraphBuilder.appearanceScreen(onNavigateBack: () -> Unit, onOpenDesignReference: () -> Unit) {
    composable<AppearanceDestination> {
        AppearanceSettingsRoute(onNavigateBack = onNavigateBack, onOpenDesignReference = onOpenDesignReference)
    }
}
