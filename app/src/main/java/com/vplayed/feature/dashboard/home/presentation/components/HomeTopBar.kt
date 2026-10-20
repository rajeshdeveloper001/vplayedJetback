package com.vplayed.feature.dashboard.home.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.vplayed.R
import com.vplayed.core.designsystem.icon.AppIcons
import com.vplayed.core.designsystem.theme.AppDesignSystem
import com.vplayed.shared.l10n.Res
import com.vplayed.shared.l10n.home_open_appearance
import com.vplayed.shared.l10n.menu_open
import org.jetbrains.compose.resources.stringResource

/** Width / height of the logo vector (viewport 500 × 95). */
private const val LOGO_ASPECT_RATIO = 500f / 95f

/** Home app bar: side-menu button, brand logo (matching the app theme, not the system theme) and settings. */
@OptIn(ExperimentalMaterial3Api::class) // Material3 1.4 top app bars are still experimental.
@Composable
internal fun HomeTopBar(
    onOpenMenu: () -> Unit,
    onOpenAppearance: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    val logo = if (AppDesignSystem.isDark) R.drawable.ic_vplayed_logo_on_dark else R.drawable.ic_vplayed_logo_on_light
    CenterAlignedTopAppBar(
        title = {
            Image(
                painter = painterResource(logo),
                contentDescription = stringResource(R.string.splash_logo_description),
                modifier = Modifier
                    .height(AppDesignSystem.dimens.topBarLogoHeight)
                    .aspectRatio(LOGO_ASPECT_RATIO),
            )
        },
        navigationIcon = {
            IconButton(
                onClick = onOpenMenu,
                modifier = Modifier.testTag(HomeTestTags.MENU),
            ) {
                Icon(painterResource(AppIcons.Menu), contentDescription = stringResource(Res.string.menu_open))
            }
        },
        actions = {
            IconButton(
                onClick = onOpenAppearance,
                modifier = Modifier.testTag(HomeTestTags.APPEARANCE),
            ) {
                Icon(painterResource(AppIcons.Tune), contentDescription = stringResource(Res.string.home_open_appearance))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = AppDesignSystem.colors.background,
            scrolledContainerColor = AppDesignSystem.colors.surfaceContainer,
        ),
        scrollBehavior = scrollBehavior,
    )
}
