package com.vplayed.feature.auth.presentation.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vplayed.R
import com.vplayed.core.designsystem.theme.AppDesignSystem
import com.vplayed.core.designsystem.theme.AppTheme
import com.vplayed.core.ui.preview.ScreenPreviews
import com.vplayed.feature.auth.presentation.components.AuthBackButton
import com.vplayed.feature.auth.presentation.components.AuthInlineError
import com.vplayed.feature.auth.presentation.components.AuthLayout
import com.vplayed.feature.auth.presentation.components.AuthSuccessStep
import com.vplayed.feature.auth.presentation.components.DeviceLimitStep
import com.vplayed.feature.auth.presentation.components.LoginPrimaryButton
import com.vplayed.feature.auth.presentation.components.OtpCodeField
import com.vplayed.feature.auth.presentation.components.OtpTestTags
import com.vplayed.feature.auth.presentation.components.shakeOnChange
import com.vplayed.feature.auth.presentation.viewmodel.OtpUiState
import com.vplayed.feature.auth.presentation.viewmodel.OtpViewModel
import com.vplayed.feature.auth.presentation.viewmodel.textOf
import com.vplayed.shared.auth.AuthMessage
import com.vplayed.shared.auth.DeviceKind
import com.vplayed.shared.auth.DeviceSignOutKey
import com.vplayed.shared.auth.OtpStep
import com.vplayed.shared.auth.SignedInDevice

/** Callbacks of the OTP screen. */
data class OtpActions(
    val onCodeChange: (String) -> Unit = {},
    val onVerify: () -> Unit = {},
    val onResend: () -> Unit = {},
    /** Back arrow, "Change" and the system Back button. */
    val onBack: () -> Unit = {},
    val onSignOutDevice: (SignedInDevice) -> Unit = {},
    val onSignOutAllDevices: () -> Unit = {},
    val onContinueAfterSignOut: () -> Unit = {},
    /** The close button: leave sign-in altogether. */
    val onClose: () -> Unit = {},
)

// Prototype spacing: title→note 28, note→boxes 24, boxes→resend 22, resend→button 22.
private val StepLabelSpacing = 12.dp
private val BackButtonBottomSpacing = 8.dp
private val TitleBottomSpacing = 28.dp
private val NoteBottomSpacing = 24.dp
private val BoxesBottomSpacing = 22.dp
private val ResendBottomSpacing = 22.dp
private val ErrorTopSpacing = 10.dp

/**
 * Verification steps of email login (prototype "VPlayed Sign Up" steps 2–3 and the device-limit
 * step) in the shared sign-in layout. System Back is handled by the ViewModel, so Back on the
 * device step returns to the code step like the prototype's Back control.
 */
@Composable
fun OtpScreen(
    uiState: OtpUiState,
    actions: OtpActions,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = actions.onBack)
    AuthLayout(
        onClose = actions.onClose,
        screenTag = OtpTestTags.SCREEN,
        cardTag = OtpTestTags.CARD,
        modifier = modifier,
    ) { _ ->
        // A refused code shakes the card once (prototype `.wpm-card.shake`).
        Column(modifier = Modifier.fillMaxWidth().shakeOnChange(uiState.shakeCount)) {
            val error = uiState.error?.let { AuthMessage.textOf(it) }
            when (uiState.step) {
                OtpStep.Code -> CodeStep(uiState, error, actions)
                OtpStep.Devices -> DeviceLimitStep(
                    state = uiState,
                    error = error,
                    onBack = actions.onBack,
                    onSignOutDevice = actions.onSignOutDevice,
                    onPrimary = if (uiState.hasSignedOutDevice) actions.onContinueAfterSignOut else actions.onSignOutAllDevices,
                )
                OtpStep.Success -> AuthSuccessStep(notice = uiState.notice?.let { AuthMessage.textOf(it) })
            }
        }
    }
}

@Composable
private fun ColumnScope.CodeStep(uiState: OtpUiState, error: String?, actions: OtpActions) {
    val auth = AppDesignSystem.extendedColors.auth
    val textStyles = AppDesignSystem.textStyles
    AuthBackButton(onClick = actions.onBack)
    Spacer(Modifier.height(BackButtonBottomSpacing))
    Text(
        text = stringResource(R.string.otp_step_label).uppercase(),
        style = textStyles.badge,
        color = AppDesignSystem.extendedColors.formAccent,
        // The prototype announces each step ("Step 2 of 3, verify your code").
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
    Spacer(Modifier.height(StepLabelSpacing))
    Text(
        text = stringResource(R.string.otp_title),
        style = textStyles.formTitle,
        color = auth.textStrong,
        modifier = Modifier.semantics { heading() },
    )
    Spacer(Modifier.height(TitleBottomSpacing))
    val note = stringResource(R.string.otp_note, uiState.email)
    Text(
        text = buildAnnotatedString {
            append(note)
            // The email is shown in full and emphasized (the prototype does not mask it).
            val start = note.indexOf(uiState.email)
            if (start >= 0) {
                addStyle(SpanStyle(color = auth.textStrong, fontWeight = FontWeight.Bold), start, start + uiState.email.length)
            }
        },
        style = AppDesignSystem.typography.bodyMedium,
        color = auth.textSecondary,
    )
    // "Change" sits on its own line with a 48dp touch target, like the prototype's ≤ 900px layout.
    Text(
        text = stringResource(R.string.otp_change),
        style = AppDesignSystem.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
        color = AppDesignSystem.colors.primary,
        modifier = Modifier
            .testTag(OtpTestTags.CHANGE)
            .minimumInteractiveComponentSize()
            .clickable(role = Role.Button, onClick = actions.onBack),
    )
    Spacer(Modifier.height(NoteBottomSpacing - AppDesignSystem.dimens.spacing.xxs))
    OtpCodeField(
        code = uiState.code,
        length = OtpViewModel.CODE_LENGTH,
        errorText = error,
        enabled = !uiState.isVerifying,
        onCodeChange = actions.onCodeChange,
        onDone = actions.onVerify,
    )
    if (error != null) {
        // Prototype `.wpm-err`: 10dp under the boxes; the gap to the next line shrinks by the same amount.
        Spacer(Modifier.height(ErrorTopSpacing))
        AuthInlineError(text = error)
        Spacer(Modifier.height(BoxesBottomSpacing - ErrorTopSpacing))
    } else {
        Spacer(Modifier.height(BoxesBottomSpacing))
    }
    ResendLine(uiState = uiState, onResend = actions.onResend)
    Spacer(Modifier.height(ResendBottomSpacing))
    LoginPrimaryButton(
        text = stringResource(R.string.otp_verify),
        loading = uiState.isVerifying,
        enabled = uiState.code.length == OtpViewModel.CODE_LENGTH,
        onClick = actions.onVerify,
        modifier = Modifier.testTag(OtpTestTags.VERIFY),
    )
}

/** "Didn't get the code? Resend in 0:59": Resend is dimmed with the countdown beside it until it can be used. */
@Composable
private fun ResendLine(uiState: OtpUiState, onResend: () -> Unit) {
    val auth = AppDesignSystem.extendedColors.auth
    val style = AppDesignSystem.textStyles.formSupporting
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(AppDesignSystem.dimens.spacing.xxs),
        verticalArrangement = Arrangement.Center,
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = stringResource(R.string.otp_resend_prompt), style = style, color = auth.textSecondary)
        Text(
            text = stringResource(R.string.otp_resend),
            style = style.copy(fontWeight = if (uiState.canResend) FontWeight.Bold else FontWeight.SemiBold),
            color = if (uiState.canResend) AppDesignSystem.colors.primary else auth.textMuted,
            modifier = Modifier
                .testTag(OtpTestTags.RESEND)
                .minimumInteractiveComponentSize()
                .clickable(enabled = uiState.canResend, role = Role.Button, onClick = onResend),
        )
        if (uiState.resendSecondsLeft > 0) {
            Text(
                text = stringResource(R.string.otp_resend_in, uiState.resendSecondsLeft / 60, uiState.resendSecondsLeft % 60),
                style = style,
                color = auth.textSecondary,
                modifier = Modifier.testTag(OtpTestTags.RESEND_COUNTDOWN),
            )
        }
    }
}

private val previewDevices = listOf(
    SignedInDevice("Chrome on Linux", DeviceKind.DESKTOP, "Chrome", true, DeviceSignOutKey("a", "b")),
    SignedInDevice("iPhone 15", DeviceKind.PHONE, null, false, DeviceSignOutKey("c", "d")),
)

@ScreenPreviews
@Composable
private fun OtpScreenCodePreview() {
    AppTheme { OtpScreen(OtpUiState(email = "user@example.com", code = "123", resendSecondsLeft = 42), OtpActions()) }
}

@ScreenPreviews
@Composable
private fun OtpScreenDevicesPreview() {
    AppTheme { OtpScreen(OtpUiState(email = "user@example.com", step = OtpStep.Devices, devices = previewDevices), OtpActions()) }
}

@ScreenPreviews
@Composable
private fun OtpScreenSuccessPreview() {
    AppTheme { OtpScreen(OtpUiState(email = "user@example.com", step = OtpStep.Success), OtpActions()) }
}
