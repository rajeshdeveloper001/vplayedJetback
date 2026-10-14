package com.vplayed.reference.showcase.presentation.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.vplayed.reference.showcase.presentation.screen.DesignShowcaseRoute
import kotlinx.serialization.Serializable

@Serializable
data object DesignShowcaseDestination

/** Design-system reference (opened from Appearance settings). Not part of the product UI. */
fun NavGraphBuilder.designShowcaseScreen(onNavigateBack: () -> Unit) {
    composable<DesignShowcaseDestination> { DesignShowcaseRoute(onNavigateBack = onNavigateBack) }
}
