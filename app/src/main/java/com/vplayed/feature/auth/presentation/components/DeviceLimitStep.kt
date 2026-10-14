package com.vplayed.feature.auth.presentation.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vplayed.R
import com.vplayed.core.designsystem.icon.AppIcons
import com.vplayed.core.designsystem.theme.AppDesignSystem
import com.vplayed.core.designsystem.theme.AppTheme
import com.vplayed.feature.auth.domain.model.DeviceKind
import com.vplayed.feature.auth.domain.model.DeviceSignOutKey
import com.vplayed.feature.auth.domain.model.SignedInDevice
import com.vplayed.feature.auth.presentation.viewmodel.OtpUiState

// Prototype `.dev-row`: 13/15 padding, 10dp corners, 13dp gaps; `.dev-list` 10dp apart.
private val RowVerticalPadding = 13.dp
private val RowHorizontalPadding = 15.dp
private val RowGap = 13.dp
private val RowCorner = 10.dp
private val ListGap = 10.dp
private val ListBottomSpacing = 20.dp
private val TitleBottomSpacing = 28.dp
private val IconTileCorner = 8.dp
private val SignOutHorizontalPadding = 14.dp
private const val SIGN_OUT_BORDER_ALPHA = 0.45f
private const val DISABLED_ALPHA = 0.5f

/**
 * Device-limit step (prototype `data-step="devices"`): the account is signed in on its maximum
 * number of devices. Each device can be signed out; once at least one is, the primary button
 * continues the login, until then it signs every device out.
 */
@Composable
internal fun ColumnScope.DeviceLimitStep(
    state: OtpUiState,
    error: String?,
    onBack: () -> Unit,
    onSignOutDevice: (SignedInDevice) -> Unit,
    onPrimary: () -> Unit,
) {
    val auth = AppDesignSystem.extendedColors.auth
    AuthBackButton(onClick = onBack)
    Spacer(Modifier.height(AppDesignSystem.dimens.spacing.xs))
    Text(
        text = stringResource(R.string.otp_devices_title),
        style = AppDesignSystem.textStyles.formTitle,
        color = auth.textStrong,
        modifier = Modifier.semantics { heading() },
    )
    Spacer(Modifier.height(AppDesignSystem.dimens.spacing.sm))
    Text(
        text = pluralStringResource(R.plurals.otp_devices_note, state.devices.size, state.devices.size),
        style = AppDesignSystem.typography.bodyMedium,
        color = auth.textSecondary,
    )
    Spacer(Modifier.height(TitleBottomSpacing - AppDesignSystem.dimens.spacing.sm))
    Column(
        modifier = Modifier.fillMaxWidth().testTag(OtpTestTags.DEVICES),
        verticalArrangement = Arrangement.spacedBy(ListGap),
    ) {
        state.devices.forEachIndexed { index, device ->
            DeviceRow(
                device = device,
                enabled = !state.isSigningOut,
                onSignOut = { onSignOutDevice(device) },
                signOutTag = OtpTestTags.DEVICE_SIGN_OUT_PREFIX + index,
            )
        }
    }
    if (error != null) {
        Spacer(Modifier.height(AppDesignSystem.dimens.spacing.sm))
        AuthInlineError(text = error)
        Spacer(Modifier.height(ListBottomSpacing - AppDesignSystem.dimens.spacing.sm))
    } else {
        Spacer(Modifier.height(ListBottomSpacing))
    }
    LoginPrimaryButton(
        text = stringResource(if (state.hasSignedOutDevice) R.string.otp_devices_continue else R.string.otp_devices_sign_out_all),
        loading = state.isSigningOut,
        onClick = onPrimary,
        showArrow = state.hasSignedOutDevice,
        modifier = Modifier.testTag(OtpTestTags.DEVICES_PRIMARY),
    )
}

@Composable
private fun DeviceRow(device: SignedInDevice, enabled: Boolean, onSignOut: () -> Unit, signOutTag: String) {
    val auth = AppDesignSystem.extendedColors.auth
    val dimens = AppDesignSystem.dimens
    val rowShape = androidx.compose.foundation.shape.RoundedCornerShape(RowCorner)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(auth.fieldFill, rowShape)
            .border(dimens.borderThin, auth.socialBorder, rowShape)
            .padding(horizontal = RowHorizontalPadding, vertical = RowVerticalPadding),
        horizontalArrangement = Arrangement.spacedBy(RowGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(dimens.authIconButtonSize)
                .background(auth.iconTileFill, androidx.compose.foundation.shape.RoundedCornerShape(IconTileCorner)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(device.kind.icon()),
                contentDescription = null,
                tint = auth.textSecondary,
                modifier = Modifier.size(dimens.icon.medium),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = device.name,
                style = AppDesignSystem.textStyles.cardTitle,
                color = auth.textStrong,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val sub = listOfNotNull(
                stringResource(R.string.otp_device_this_device).takeIf { device.isCurrent },
                device.detail,
            ).joinToString(" · ")
            if (sub.isNotEmpty()) {
                Text(
                    text = sub,
                    style = AppDesignSystem.textStyles.cardSubtitle,
                    color = auth.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        SignOutButton(enabled = enabled, onClick = onSignOut, modifier = Modifier.testTag(signOutTag))
    }
}

/** Prototype `.dev-out`: outlined brand button, 36dp tall inside a 48dp touch target. */
@Composable
private fun SignOutButton(enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val brand = AppDesignSystem.colors.primary
    val shape = AppDesignSystem.shapes.button
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .alpha(if (enabled) 1f else DISABLED_ALPHA),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .heightIn(min = AppDesignSystem.dimens.deviceSignOutHeight)
                .clip(shape)
                .border(AppDesignSystem.dimens.borderThin, brand.copy(alpha = SIGN_OUT_BORDER_ALPHA), shape)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .padding(horizontal = SignOutHorizontalPadding),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.otp_device_sign_out),
                style = AppDesignSystem.textStyles.actionSmall,
                color = brand,
            )
        }
    }
}

@DrawableRes
private fun DeviceKind.icon(): Int = when (this) {
    DeviceKind.DESKTOP -> AppIcons.DesktopWindows
    DeviceKind.PHONE -> AppIcons.PhoneIphone
    DeviceKind.TV -> AppIcons.Tv
    DeviceKind.OTHER -> AppIcons.Devices
}

@Preview(showBackground = true)
@Composable
private fun DeviceLimitStepPreview() {
    AppTheme {
        Column(Modifier.background(AppDesignSystem.extendedColors.auth.canvas).padding(AppDesignSystem.dimens.spacing.md)) {
            DeviceLimitStep(
                state = OtpUiState(
                    email = "user@example.com",
                    devices = listOf(
                        SignedInDevice("Chrome on Linux", DeviceKind.DESKTOP, "Chrome", true, DeviceSignOutKey("a", "b")),
                        SignedInDevice("iPhone 15", DeviceKind.PHONE, null, false, DeviceSignOutKey("c", "d")),
                    ),
                ),
                error = null,
                onBack = {},
                onSignOutDevice = {},
                onPrimary = {},
            )
        }
    }
}
