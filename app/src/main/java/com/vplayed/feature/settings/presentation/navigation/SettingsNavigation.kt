package com.vplayed.feature.settings.presentation.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.vplayed.feature.settings.presentation.screen.AppearanceSettingsRoute
import com.vplayed.feature.settings.presentation.screen.LanguageRoute
import kotlinx.serialization.Serializable

@Serializable
data object AppearanceDestination

/** Menu → Language: the app language, from the backend's list. */
@Serializable
data object LanguageDestination

fun NavGraphBuilder.appearanceScreen(onNavigateBack: () -> Unit, onOpenDesignReference: () -> Unit) {
    composable<AppearanceDestination> {
        AppearanceSettingsRoute(onNavigateBack = onNavigateBack, onOpenDesignReference = onOpenDesignReference)
    }
}

fun NavGraphBuilder.languageScreen(onNavigateBack: () -> Unit) {
    composable<LanguageDestination> {
        LanguageRoute(onNavigateBack = onNavigateBack)
    }
}
