package com.vplayed.feature.auth.presentation.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vplayed.R
import com.vplayed.core.designsystem.icon.AppIcons
import com.vplayed.core.designsystem.theme.AppDesignSystem
import com.vplayed.core.designsystem.theme.AppTheme
import com.vplayed.core.ui.preview.ComponentPreviews

// Prototype `.btn-primary`: 15/24 padding, red glow shadow (35%), 2dp spinner ring at 40%.
private val PrimaryVerticalPadding = 15.dp
private val PrimaryShadowElevation = 12.dp
private const val PRIMARY_SHADOW_ALPHA = 0.35f
private val SpinnerSize = 18.dp
private val SpinnerStroke = 2.dp
private const val SPINNER_TRACK_ALPHA = 0.4f
// Prototype `.social-btn`: 13/18 padding, 18dp mark; disabled controls use 50% opacity.
private val SocialVerticalPadding = 13.dp
private val SocialIconSize = 18.dp
private const val DISABLED_ALPHA = 0.5f
private val ActionIconSize = 20.dp

/**
 * Full-width primary action with an optional trailing arrow; shows a spinner and ignores taps while
 * [loading]. When not [enabled] it is drawn at half opacity and ignores taps (prototype `:disabled`).
 */
@Composable
internal fun LoginPrimaryButton(
    text: String,
    loading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showArrow: Boolean = true,
) {
    val colors = AppDesignSystem.colors
    val dimens = AppDesignSystem.dimens
    val shape = AppDesignSystem.shapes.button
    val loadingDescription = stringResource(R.string.cd_loading)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = dimens.formActionMinHeight)
            .alpha(if (enabled && !loading) 1f else if (loading) 0.85f else DISABLED_ALPHA)
            .shadow(
                elevation = PrimaryShadowElevation,
                shape = shape,
                ambientColor = colors.primary.copy(alpha = PRIMARY_SHADOW_ALPHA),
                spotColor = colors.primary.copy(alpha = PRIMARY_SHADOW_ALPHA),
            )
            .clip(shape)
            .background(colors.primary)
            .clickable(enabled = enabled && !loading, role = Role.Button, onClick = onClick)
            .semantics { if (loading) stateDescription = loadingDescription }
            .padding(horizontal = dimens.spacing.xl, vertical = PrimaryVerticalPadding),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(dimens.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(SpinnerSize),
                    color = colors.onPrimary,
                    trackColor = colors.onPrimary.copy(alpha = SPINNER_TRACK_ALPHA),
                    strokeWidth = SpinnerStroke,
                )
            }
            Text(text = text, style = AppDesignSystem.textStyles.formAction, color = colors.onPrimary)
            if (!loading && showArrow) {
                Icon(
                    painter = painterResource(AppIcons.ArrowForward),
                    contentDescription = null,
                    tint = colors.onPrimary,
                    modifier = Modifier.size(ActionIconSize),
                )
            }
        }
    }
}

/**
 * Outlined glass button (prototype `.social-btn`), used for social sign-in and "Continue as guest".
 * [iconTint] null draws the icon in its own colors (Google mark).
 */
@Composable
internal fun LoginSocialButton(
    text: String,
    @DrawableRes icon: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    iconTint: Color? = AppDesignSystem.extendedColors.auth.textStrong,
) {
    val auth = AppDesignSystem.extendedColors.auth
    val dimens = AppDesignSystem.dimens
    val shape = AppDesignSystem.shapes.button
    val loadingDescription = stringResource(R.string.cd_loading)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = dimens.socialButtonMinHeight)
            .alpha(if (enabled && !loading) 1f else if (loading) 0.85f else DISABLED_ALPHA)
            .clip(shape)
            .background(auth.socialFill)
            .border(dimens.borderThin, auth.socialBorder, shape)
            .clickable(enabled = enabled && !loading, role = Role.Button, onClick = onClick)
            .semantics { if (loading) stateDescription = loadingDescription }
            .padding(horizontal = dimens.spacing.md, vertical = SocialVerticalPadding),
        horizontalArrangement = Arrangement.spacedBy(dimens.spacing.sm, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(SocialIconSize),
                color = auth.textStrong,
                strokeWidth = SpinnerStroke,
            )
        } else {
            val painter = painterResource(icon)
            if (iconTint == null) {
                Image(painter, contentDescription = null, modifier = Modifier.size(SocialIconSize))
            } else {
                Icon(painter, contentDescription = null, tint = iconTint, modifier = Modifier.size(SocialIconSize))
            }
        }
        Text(
            text = text,
            style = AppDesignSystem.textStyles.formAction,
            color = auth.textStrong,
            textAlign = TextAlign.Center,
        )
    }
}

/** Round glass close button (prototype `.close-btn`, 38dp) with a 48dp touch target. */
@Composable
internal fun LoginCloseButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    AuthRoundButton(
        icon = AppIcons.Close,
        description = stringResource(R.string.login_close),
        size = AppDesignSystem.dimens.authIconButtonSize,
        onClick = onClick,
        modifier = modifier.testTag(LoginTestTags.CLOSE),
    )
}

/** Round glass back button of the verification screens (prototype ≤ 900px `.astep-back` disc, 40dp). */
@Composable
internal fun AuthBackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    AuthRoundButton(
        icon = AppIcons.ChevronLeft,
        description = stringResource(R.string.otp_back),
        size = AppDesignSystem.dimens.authBackButtonSize,
        onClick = onClick,
        modifier = modifier.testTag(OtpTestTags.BACK),
    )
}

@Composable
private fun AuthRoundButton(
    @DrawableRes icon: Int,
    description: String,
    size: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val auth = AppDesignSystem.extendedColors.auth
    val dimens = AppDesignSystem.dimens
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(auth.iconTileFill)
                .border(dimens.borderThin, auth.iconTileBorder, CircleShape)
                .clickable(role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = auth.textPrimary,
                modifier = Modifier.size(ActionIconSize),
            )
        }
    }
}

@ComponentPreviews
@Composable
private fun LoginButtonsPreview() {
    AppTheme {
        Column(
            modifier = Modifier
                .background(AppDesignSystem.extendedColors.auth.canvas)
                .padding(AppDesignSystem.dimens.spacing.md),
            verticalArrangement = Arrangement.spacedBy(AppDesignSystem.dimens.spacing.sm),
        ) {
            LoginPrimaryButton(text = "Continue", loading = false, onClick = {})
            LoginPrimaryButton(text = "Continue", loading = true, onClick = {})
            LoginSocialButton(text = "Sign in with Google", icon = AppIcons.Google, onClick = {}, iconTint = null, enabled = false)
            LoginSocialButton(text = "Continue as guest", icon = AppIcons.PersonOutline, onClick = {})
            LoginCloseButton(onClick = {})
        }
    }
}
