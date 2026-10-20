package com.vplayed.feature.auth.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vplayed.core.designsystem.icon.AppIcons
import com.vplayed.core.designsystem.theme.AppDesignSystem
import com.vplayed.core.designsystem.theme.AppTheme
import com.vplayed.core.ui.localizedNumber
import com.vplayed.core.ui.markdownLinks
import com.vplayed.core.ui.preview.ScreenPreviews
import com.vplayed.feature.auth.presentation.components.AuthLayout
import com.vplayed.feature.auth.presentation.components.AuthLayoutMode
import com.vplayed.feature.auth.presentation.components.AuthSuccessStep
import com.vplayed.feature.auth.presentation.components.LoginEmailField
import com.vplayed.feature.auth.presentation.components.LoginPhoneField
import com.vplayed.feature.auth.presentation.components.LoginPrimaryButton
import com.vplayed.feature.auth.presentation.components.LoginSocialButton
import com.vplayed.feature.auth.presentation.components.LoginTestTags
import com.vplayed.feature.auth.presentation.viewmodel.LoginOptions
import com.vplayed.feature.auth.presentation.viewmodel.LoginUiState
import com.vplayed.feature.auth.presentation.viewmodel.textOf
import com.vplayed.shared.auth.AuthMessage
import com.vplayed.shared.auth.SocialProvider
import com.vplayed.shared.l10n.Res
import com.vplayed.shared.l10n.common_continue
import com.vplayed.shared.l10n.common_sign_in
import com.vplayed.shared.l10n.login_apple
import com.vplayed.shared.l10n.login_continue_as_guest
import com.vplayed.shared.l10n.login_divider_email
import com.vplayed.shared.l10n.login_divider_phone
import com.vplayed.shared.l10n.login_email_help
import com.vplayed.shared.l10n.login_email_label
import com.vplayed.shared.l10n.login_email_placeholder
import com.vplayed.shared.l10n.login_google
import com.vplayed.shared.l10n.login_have_account
import com.vplayed.shared.l10n.login_phone_country
import com.vplayed.shared.l10n.login_phone_help
import com.vplayed.shared.l10n.login_phone_label
import com.vplayed.shared.l10n.login_step
import com.vplayed.shared.l10n.login_terms
import com.vplayed.shared.l10n.login_title
import com.vplayed.shared.l10n.message
import com.vplayed.shared.l10n.signInMessage
import com.vplayed.shared.phone.CountryDialCode
import com.vplayed.shared.phone.CountryDialCodes
import com.vplayed.shared.validation.EmailValidationResult
import org.jetbrains.compose.resources.stringResource

/** Callbacks of the login screen. */
data class LoginActions(
    val onEmailChange: (String) -> Unit = {},
    val onEmailFocusLost: () -> Unit = {},
    val onPhoneChange: (String) -> Unit = {},
    val onPhoneFocusLost: () -> Unit = {},
    val onCountryChange: (CountryDialCode) -> Unit = {},
    val onSubmit: () -> Unit = {},
    val onGoogle: () -> Unit = {},
    /** "Continue as guest": a guest session, then back to the dashboard. */
    val onGuest: () -> Unit = {},
    /** Close button: back to the dashboard. */
    val onClose: () -> Unit = {},
)

// Spacing inside the form (prototype `.auth-head`, `.socials`, `.field`, `.btn-primary`, `.terms`).
private val StepLabelSpacing = 12.dp
private val TitleBottomSpacing = 28.dp
private val SocialGap = 10.dp
private val SocialBottomSpacing = 22.dp
private val DividerTopSpacing = 4.dp
private val DividerTextGap = 14.dp
private val FormGap = 16.dp
private val ActionTopSpacing = 6.dp
private val SignInLineTopSpacing = 18.dp

/**
 * Sign-in, step 1 (prototype "VPlayed Sign Up"): Google, then the email field — or the mobile
 * number field when [LoginOptions.usesPhone] — and guest access when the server allows it. A
 * centered glass card on phones, foldables and landscape phones; a two-column modal with the
 * marketing column on wide windows (see [AuthLayout]). Scrolls when the window (or the space left by
 * the keyboard) is shorter than the content.
 */
@Composable
fun LoginScreen(
    uiState: LoginUiState,
    actions: LoginActions,
    modifier: Modifier = Modifier,
    options: LoginOptions = LoginOptions(),
) {
    AuthLayout(
        onClose = actions.onClose,
        screenTag = LoginTestTags.SCREEN,
        cardTag = LoginTestTags.CARD,
        modifier = modifier,
    ) { mode ->
        if (options.socialSucceeded) {
            AuthSuccessStep(notice = options.notice?.let { AuthMessage.textOf(it) })
        } else {
            LoginForm(uiState, options, actions, compactFooter = mode == AuthLayoutMode.SingleCard)
        }
    }
}

@Composable
private fun ColumnScope.LoginForm(
    uiState: LoginUiState,
    options: LoginOptions,
    actions: LoginActions,
    compactFooter: Boolean,
) {
    val auth = AppDesignSystem.extendedColors.auth
    val textStyles = AppDesignSystem.textStyles
    val colors = AppDesignSystem.colors
    val dimens = AppDesignSystem.dimens
    Text(
        text = stringResource(Res.string.login_step, localizedNumber(1)).uppercase(),
        style = textStyles.badge,
        color = AppDesignSystem.extendedColors.formAccent,
    )
    Spacer(Modifier.height(StepLabelSpacing))
    Text(
        text = stringResource(Res.string.login_title),
        style = textStyles.formTitle,
        color = auth.textStrong,
        modifier = Modifier.semantics { heading() },
    )
    Spacer(Modifier.height(TitleBottomSpacing))

    val busy = uiState.isSubmitting || options.phone.isSubmitting || uiState.isJoiningAsGuest || options.socialInFlight != null
    val isGoogleLoading = options.socialInFlight == SocialProvider.GOOGLE
    val isGuestLoading = uiState.isJoiningAsGuest
    Column(verticalArrangement = Arrangement.spacedBy(SocialGap)) {
        if (options.googleAvailable) {
            LoginSocialButton(
                text = stringResource(Res.string.login_google),
                icon = AppIcons.Google,
                iconTint = null,
                loading = isGoogleLoading,
                enabled = !busy || isGoogleLoading,
                onClick = actions.onGoogle,
                modifier = Modifier.testTag(LoginTestTags.GOOGLE),
            )
        }
        // Sign in with Apple has no native Android flow (the previous apps offer it on iOS only).
        LoginSocialButton(
            text = stringResource(Res.string.login_apple),
            icon = AppIcons.Apple,
            enabled = false,
            onClick = {},
        )
    }
    Spacer(Modifier.height(SocialBottomSpacing + DividerTopSpacing))
    OrDivider(text = stringResource(if (options.usesPhone) Res.string.login_divider_phone else Res.string.login_divider_email))
    Spacer(Modifier.height(dimens.spacing.lg))

    if (options.usesPhone) {
        // The shared rule (as VPlayed): India's ten-digit message, the previous app's for the others.
        val phoneError = options.phone.numberError
            ?.message(isIndia = options.phone.country.dialCode == CountryDialCodes.INDIA.dialCode, isRegistration = false)
            ?.let { stringResource(it) }
        LoginPhoneField(
            value = options.phone.formattedNumber,
            onValueChange = actions.onPhoneChange,
            country = options.phone.country,
            countries = CountryDialCodes.all,
            onCountryChange = actions.onCountryChange,
            label = stringResource(Res.string.login_phone_label),
            countryLabel = stringResource(Res.string.login_phone_country),
            helpText = stringResource(Res.string.login_phone_help),
            errorMessage = phoneError,
            readOnly = options.phone.isSubmitting,
            onImeAction = actions.onSubmit,
            onFocusLost = actions.onPhoneFocusLost,
        )
    } else {
        // The field is capped at the maximum length, so every error is about the address (shared rule).
        val errorMessage = uiState.emailError?.signInMessage?.let { stringResource(it) }
        LoginEmailField(
            value = uiState.email,
            onValueChange = actions.onEmailChange,
            label = stringResource(Res.string.login_email_label),
            placeholder = stringResource(Res.string.login_email_placeholder),
            helpText = stringResource(Res.string.login_email_help),
            errorMessage = errorMessage,
            readOnly = uiState.isSubmitting,
            onImeAction = actions.onSubmit,
            onFocusLost = actions.onEmailFocusLost,
        )
    }
    Spacer(Modifier.height(FormGap + ActionTopSpacing))
    LoginPrimaryButton(
        text = stringResource(Res.string.common_continue),
        loading = uiState.isSubmitting || options.phone.isSubmitting,
        onClick = actions.onSubmit,
        modifier = Modifier.testTag(LoginTestTags.SUBMIT),
    )
    Spacer(Modifier.height(FormGap + ActionTopSpacing))
    Text(text = termsText(colors.primary), style = textStyles.formSupporting, color = auth.textPrimary)

    if (options.guestAllowed) {
        Spacer(Modifier.height(FormGap + ActionTopSpacing))
        LoginSocialButton(
            text = stringResource(Res.string.login_continue_as_guest),
            icon = AppIcons.PersonOutline,
            loading = isGuestLoading,
            enabled = !busy || isGuestLoading,
            onClick = actions.onGuest,
            modifier = Modifier.testTag(LoginTestTags.GUEST),
        )
    }

    if (compactFooter) {
        Spacer(Modifier.height(SignInLineTopSpacing))
        val signIn = stringResource(Res.string.common_sign_in)
        Text(
            text = highlighted(stringResource(Res.string.login_have_account, signIn), listOf(signIn), colors.primary, FontWeight.Bold),
            style = AppDesignSystem.typography.bodyMedium,
            color = auth.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun OrDivider(text: String) {
    val auth = AppDesignSystem.extendedColors.auth
    Row(verticalAlignment = Alignment.CenterVertically) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = auth.iconTileBorder)
        Text(
            text = text,
            style = AppDesignSystem.textStyles.formDivider,
            color = auth.textMuted,
            modifier = Modifier.padding(horizontal = DividerTextGap),
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = auth.iconTileBorder)
    }
}

/** The shared terms line; its links are Markdown (`[Terms of use](terms)`), drawn highlighted (no link targets yet). */
@Composable
private fun termsText(linkColor: Color): AnnotatedString {
    val (text, links) = markdownLinks(stringResource(Res.string.login_terms))
    return highlighted(text, links, linkColor, FontWeight.SemiBold)
}

/** [text] with each of [parts] drawn in [color] / [weight] (no link targets yet). */
private fun highlighted(text: String, parts: List<String>, color: Color, weight: FontWeight): AnnotatedString =
    buildAnnotatedString {
        append(text)
        parts.forEach { part ->
            val start = text.indexOf(part)
            if (start >= 0) addStyle(SpanStyle(color = color, fontWeight = weight), start, start + part.length)
        }
    }

@ScreenPreviews
@Composable
private fun LoginScreenPreview() {
    AppTheme { LoginScreen(uiState = LoginUiState(), actions = LoginActions()) }
}

@ScreenPreviews
@Composable
private fun LoginScreenPhonePreview() {
    AppTheme {
        LoginScreen(
            uiState = LoginUiState(),
            actions = LoginActions(),
            options = LoginOptions(usesPhone = true, googleAvailable = true, guestAllowed = true),
        )
    }
}

@ScreenPreviews
@Composable
private fun LoginScreenErrorPreview() {
    AppTheme {
        LoginScreen(
            uiState = LoginUiState(email = "user@", emailError = EmailValidationResult.INVALID_FORMAT),
            actions = LoginActions(),
        )
    }
}
