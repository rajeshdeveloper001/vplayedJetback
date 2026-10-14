package com.vplayed.feature.dashboard.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.vplayed.R
import com.vplayed.core.designsystem.icon.AppIcons
import com.vplayed.feature.dashboard.home.presentation.navigation.HomeDestination
import com.vplayed.feature.dashboard.home.presentation.screen.HomeRoute
import com.vplayed.feature.dashboard.promos.presentation.screen.PromosScreen
import com.vplayed.feature.dashboard.search.presentation.screen.SearchScreen
import com.vplayed.feature.dashboard.subscription.presentation.screen.SubscriptionScreen
import kotlin.reflect.KClass
import kotlinx.serialization.Serializable

@Serializable
data object SearchDestination

@Serializable
data object PromosDestination

@Serializable
data object SubscriptionDestination

/** Dashboard tabs, shown in the bottom bar (phones) or navigation rail (landscape, tablets). */
enum class DashboardTab(
    val route: Any,
    val routeClass: KClass<*>,
    @param:DrawableRes val selectedIcon: Int,
    @param:DrawableRes val unselectedIcon: Int,
    @param:StringRes val label: Int,
) {
    HOME(HomeDestination, HomeDestination::class, AppIcons.Home, AppIcons.HomeOutlined, R.string.nav_home),
    SEARCH(SearchDestination, SearchDestination::class, AppIcons.Search, AppIcons.Search, R.string.nav_search),
    PROMOS(PromosDestination, PromosDestination::class, AppIcons.Promos, AppIcons.PromosOutlined, R.string.nav_promos),
    SUBSCRIPTION(
        SubscriptionDestination,
        SubscriptionDestination::class,
        AppIcons.Subscription,
        AppIcons.SubscriptionOutlined,
        R.string.nav_subscription,
    ),
}

/** Registers the four dashboard tabs. */
fun NavGraphBuilder.dashboardGraph(onOpenMenu: () -> Unit, onOpenAppearance: () -> Unit) {
    composable<HomeDestination> { HomeRoute(onOpenMenu = onOpenMenu, onOpenAppearance = onOpenAppearance) }
    composable<SearchDestination> { SearchScreen() }
    composable<PromosDestination> { PromosScreen() }
    composable<SubscriptionDestination> { SubscriptionScreen() }
}

/** Switches tabs keeping one copy of each on the back stack and restoring its scroll state. */
fun NavController.navigateToTab(tab: DashboardTab) {
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
