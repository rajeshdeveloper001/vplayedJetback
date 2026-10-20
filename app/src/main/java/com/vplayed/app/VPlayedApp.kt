package com.vplayed.app

import android.widget.Toast
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vplayed.core.designsystem.theme.AppDesignSystem
import com.vplayed.core.ui.adaptive.rememberAppLayoutInfo
import com.vplayed.core.ui.brandName
import com.vplayed.core.ui.sharedString
import com.vplayed.feature.auth.presentation.navigation.LoginDestination
import com.vplayed.feature.auth.presentation.navigation.OtpDestination
import com.vplayed.feature.auth.presentation.navigation.loginScreen
import com.vplayed.feature.auth.presentation.navigation.otpScreen
import com.vplayed.feature.dashboard.home.presentation.navigation.HomeDestination
import com.vplayed.feature.dashboard.navigation.DashboardTab
import com.vplayed.feature.dashboard.navigation.dashboardGraph
import com.vplayed.feature.dashboard.navigation.navigateToTab
import com.vplayed.feature.settings.presentation.navigation.AppearanceDestination
import com.vplayed.feature.settings.presentation.navigation.LanguageDestination
import com.vplayed.feature.settings.presentation.navigation.appearanceScreen
import com.vplayed.feature.settings.presentation.navigation.languageScreen
import com.vplayed.feature.splashscreen.presentation.navigation.SplashDestination
import com.vplayed.feature.splashscreen.presentation.navigation.splashScreen
import com.vplayed.reference.showcase.presentation.navigation.DesignShowcaseDestination
import com.vplayed.reference.showcase.presentation.navigation.designShowcaseScreen
import com.vplayed.shared.l10n.Res
import com.vplayed.shared.l10n.session_expired
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * Root composable. Splash → dashboard (Home, Search, Promos, Subscription). The tab bar shows
 * only on dashboard tabs: a bottom bar on compact portrait windows, a navigation rail in
 * landscape and on tablets. The side menu (Home's menu button, or a swipe on any tab) opens
 * Login. Other screens (Login, Appearance, design reference) are full screen.
 */
@Composable
fun VPlayedApp(
    navController: NavHostController = rememberNavController(),
    shellViewModel: AppShellViewModel = hiltViewModel(),
) {
    val signedInEmail by shellViewModel.signedInEmail.collectAsStateWithLifecycle()
    val isSigningOut by shellViewModel.isSigningOut.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(shellViewModel) {
        shellViewModel.messages.collect { message ->
            Toast.makeText(context, sharedString(message, context.brandName()), Toast.LENGTH_SHORT).show()
        }
    }
    LaunchedEffect(shellViewModel, navController) {
        shellViewModel.sessionExpired.collect {
            Toast.makeText(context, sharedString(Res.string.session_expired), Toast.LENGTH_LONG).show()
            navController.navigate(LoginDestination) { launchSingleTop = true }
        }
    }
    val layoutInfo = rememberAppLayoutInfo()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showTabs = currentDestination?.isDashboardTab() == true
    val layoutType = when {
        !showTabs -> NavigationSuiteType.None
        layoutInfo.useNavigationRail -> NavigationSuiteType.NavigationRail
        else -> NavigationSuiteType.NavigationBar
    }
    val colors = AppDesignSystem.colors
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    // Leaving the dashboard (e.g. to Login) always closes the menu.
    LaunchedEffect(showTabs) { if (!showTabs) drawerState.snapTo(DrawerValue.Closed) }
    // Prototype tab bar: black bar, selected icon filled in the brand color with a white label,
    // others muted; no pill indicator.
    val itemColors = NavigationSuiteDefaults.itemColors(
        navigationBarItemColors = NavigationBarItemDefaults.colors(
            selectedIconColor = colors.primary,
            selectedTextColor = colors.onSurface,
            indicatorColor = Color.Transparent,
            unselectedIconColor = colors.onSurfaceVariant,
            unselectedTextColor = colors.onSurfaceVariant,
        ),
        navigationRailItemColors = NavigationRailItemDefaults.colors(
            selectedIconColor = colors.primary,
            selectedTextColor = colors.onSurface,
            indicatorColor = Color.Transparent,
            unselectedIconColor = colors.onSurfaceVariant,
            unselectedTextColor = colors.onSurfaceVariant,
        ),
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = showTabs || drawerState.isOpen,
        modifier = Modifier.semantics {
            testTagsAsResourceId = true
        },
        drawerContent = {
            AppDrawerSheet(
                signedInEmail = signedInEmail,
                onSignOut = shellViewModel::signOut,
                isSigningOut = isSigningOut,
                onLogin = {
                    scope.launch { drawerState.close() }
                    navController.navigate(LoginDestination) { launchSingleTop = true }
                },
                onLanguage = {
                    scope.launch { drawerState.close() }
                    navController.navigate(LanguageDestination) { launchSingleTop = true }
                },
            )
        },
    ) {
        NavigationSuiteScaffold(
            navigationSuiteItems = {
                DashboardTab.entries.forEach { tab ->
                    val selected = currentDestination?.hierarchy?.any { it.hasRoute(tab.routeClass) } == true
                    item(
                        selected = selected,
                        onClick = { navController.navigateToTab(tab) },
                        icon = {
                            Icon(
                                painter = painterResource(if (selected) tab.selectedIcon else tab.unselectedIcon),
                                contentDescription = null,
                            )
                        },
                        label = { Text(stringResource(tab.label)) },
                        colors = itemColors,
                    )
                }
            },
            layoutType = layoutType,
            navigationSuiteColors = NavigationSuiteDefaults.colors(
                navigationBarContainerColor = colors.background,
                navigationRailContainerColor = colors.background,
            ),
            containerColor = colors.background,
        ) {
            NavHost(
                navController = navController,
                startDestination = SplashDestination,
                // The bar/rail already pads for the system insets on its side; screens must not repeat it.
                modifier = Modifier.consumeWindowInsets(
                    when (layoutType) {
                        NavigationSuiteType.NavigationBar -> WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)
                        NavigationSuiteType.NavigationRail -> WindowInsets.safeDrawing.only(WindowInsetsSides.Start)
                        else -> WindowInsets(0)
                    },
                ),
            ) {
                splashScreen(
                    onSplashFinished = {
                        navController.navigate(HomeDestination) {
                            popUpTo<SplashDestination> { inclusive = true }
                        }
                    },
                )
                dashboardGraph(
                    onOpenMenu = { scope.launch { drawerState.open() } },
                    onOpenAppearance = { navController.navigate(AppearanceDestination) },
                )
                loginScreen(
                onClose = { navController.navigateUp() },
                onOpenOtp = { destination, startWithDeviceLimit, channel, dialCode ->
                    navController.navigate(OtpDestination(destination, startWithDeviceLimit, channel.name, dialCode)) { launchSingleTop = true }
                },
            )
            otpScreen(
                onBack = { navController.navigateUp() },
                // Close and a finished sign-in both return to the Dashboard (Home), dropping Login and OTP.
                onClose = { navController.popBackStack<HomeDestination>(inclusive = false) },
                onAuthenticated = { navController.popBackStack<HomeDestination>(inclusive = false) },
            )
                appearanceScreen(
                    onNavigateBack = { navController.navigateUp() },
                    onOpenDesignReference = { navController.navigate(DesignShowcaseDestination) },
                )
                designShowcaseScreen(onNavigateBack = { navController.navigateUp() })
                languageScreen(onNavigateBack = { navController.navigateUp() })
            }
        }
    }
}

private fun NavDestination.isDashboardTab(): Boolean =
    DashboardTab.entries.any { tab -> hierarchy.any { it.hasRoute(tab.routeClass) } }
