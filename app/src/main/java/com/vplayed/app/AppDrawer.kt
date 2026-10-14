package com.vplayed.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.vplayed.R
import com.vplayed.core.designsystem.icon.AppIcons
import com.vplayed.core.designsystem.theme.AppDesignSystem
import com.vplayed.core.designsystem.theme.AppTheme
import com.vplayed.core.ui.preview.ComponentPreviews

/** Width / height of the logo vector (viewport 500 × 95). */
private const val LOGO_ASPECT_RATIO = 500f / 95f

/** Test tags of the side menu. */
object AppDrawerTestTags {
    const val SHEET = "app_drawer"
    const val LOGIN = "app_drawer_login"
    const val SIGNED_IN = "app_drawer_signed_in"
}

/**
 * Side menu content: brand logo and the menu entries. Login while signed out; the signed-in email
 * instead once there is a session (there is nothing else to do with an account yet).
 */
@Composable
fun AppDrawerSheet(onLogin: () -> Unit, modifier: Modifier = Modifier, signedInEmail: String? = null) {
    val colors = AppDesignSystem.colors
    val spacing = AppDesignSystem.dimens.spacing
    val logo = if (AppDesignSystem.isDark) R.drawable.ic_vplayed_logo_on_dark else R.drawable.ic_vplayed_logo_on_light
    ModalDrawerSheet(
        modifier = modifier.testTag(AppDrawerTestTags.SHEET),
        drawerContainerColor = colors.surfaceContainerLow,
        drawerContentColor = colors.onSurface,
    ) {
        Image(
            painter = painterResource(logo),
            contentDescription = stringResource(R.string.splash_logo_description),
            modifier = Modifier
                .padding(horizontal = spacing.xl, vertical = spacing.xl)
                .height(AppDesignSystem.dimens.topBarLogoHeight)
                .aspectRatio(LOGO_ASPECT_RATIO),
        )
        HorizontalDivider(color = AppDesignSystem.extendedColors.hairline)
        if (signedInEmail == null) {
            NavigationDrawerItem(
                label = { Text(stringResource(R.string.menu_login), style = AppDesignSystem.textStyles.subHeader) },
                icon = { Icon(painterResource(AppIcons.Login), contentDescription = null) },
                selected = false,
                onClick = onLogin,
                colors = NavigationDrawerItemDefaults.colors(
                    unselectedContainerColor = colors.surfaceContainerLow,
                    unselectedIconColor = colors.primary,
                    unselectedTextColor = colors.onSurface,
                ),
                modifier = Modifier
                    .padding(NavigationDrawerItemDefaults.ItemPadding)
                    .padding(top = spacing.xs)
                    .testTag(AppDrawerTestTags.LOGIN),
            )
        } else {
            Text(
                text = stringResource(R.string.menu_signed_in_as, signedInEmail),
                style = AppDesignSystem.typography.bodyMedium,
                color = AppDesignSystem.extendedColors.textSoft,
                modifier = Modifier
                    .padding(horizontal = spacing.xl, vertical = spacing.md)
                    .testTag(AppDrawerTestTags.SIGNED_IN),
            )
        }
    }
}

@ComponentPreviews
@Composable
private fun AppDrawerSheetPreview() {
    AppTheme { AppDrawerSheet(onLogin = {}) }
}

@ComponentPreviews
@Composable
private fun AppDrawerSheetSignedInPreview() {
    AppTheme { AppDrawerSheet(onLogin = {}, signedInEmail = "user@example.com") }
}
