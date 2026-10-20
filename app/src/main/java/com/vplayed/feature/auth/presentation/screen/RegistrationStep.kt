package com.vplayed.feature.auth.presentation.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vplayed.R
import com.vplayed.core.designsystem.theme.AppDesignSystem
import com.vplayed.feature.auth.presentation.components.AuthBackButton
import com.vplayed.feature.auth.presentation.components.AuthInlineError
import com.vplayed.feature.auth.presentation.components.AuthTextField
import com.vplayed.feature.auth.presentation.components.LoginPhoneField
import com.vplayed.feature.auth.presentation.components.LoginPrimaryButton
import com.vplayed.feature.auth.presentation.components.RegistrationTestTags
import com.vplayed.feature.auth.presentation.viewmodel.textOf
import com.vplayed.shared.auth.AuthMessage
import com.vplayed.shared.auth.RegistrationState
import com.vplayed.shared.phone.CountryDialCode
import com.vplayed.shared.phone.CountryDialCodes
import com.vplayed.shared.validation.EmailValidationResult
import com.vplayed.shared.validation.NameValidationResult
import com.vplayed.shared.validation.PhoneValidationResult

/** Callbacks of the sign-up form. */
data class RegistrationActions(
    val onNameChange: (String) -> Unit = {},
    val onEmailChange: (String) -> Unit = {},
    val onCountryChange: (CountryDialCode) -> Unit = {},
    val onNumberChange: (String) -> Unit = {},
    val onShowReferral: () -> Unit = {},
    val onReferralChange: (String) -> Unit = {},
    val onSubmit: () -> Unit = {},
    /** Back arrow and the system Back button: leave sign-up. */
    val onBack: () -> Unit = {},
)

private val BackButtonBottomSpacing = 8.dp
private val StepLabelSpacing = 12.dp
private val TitleBottomSpacing = 12.dp
private val NoteBottomSpacing = 24.dp
private val FieldGap = 18.dp
private val ButtonTopSpacing = 24.dp
private val ErrorTopSpacing = 10.dp

/**
 * The sign-up form of a new account (UIKit `RegisterViewController`; prototype "Complete your
 * profile"): the name, then the mobile number (email accounts) or the email (phone accounts); what
 * the account signed in with is shown read-only. Email accounts can add a referral code.
 */
@Composable
internal fun ColumnScope.RegistrationForm(state: RegistrationState, actions: RegistrationActions) {
    val auth = AppDesignSystem.extendedColors.auth
    val textStyles = AppDesignSystem.textStyles
    val locked = state.isSubmitting

    AuthBackButton(onClick = actions.onBack)
    Spacer(Modifier.height(BackButtonBottomSpacing))
    Text(
        text = stringResource(R.string.otp_step_3_label).uppercase(),
        style = textStyles.badge,
        color = AppDesignSystem.extendedColors.formAccent,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
    Spacer(Modifier.height(StepLabelSpacing))
    Text(
        text = stringResource(R.string.register_title),
        style = textStyles.formTitle,
        color = auth.textStrong,
        modifier = Modifier.semantics { heading() },
    )
    Spacer(Modifier.height(TitleBottomSpacing))
    Text(
        text = stringResource(R.string.register_note),
        style = AppDesignSystem.typography.bodyMedium,
        color = auth.textSecondary,
    )
    Spacer(Modifier.height(NoteBottomSpacing))

    Column(modifier = Modifier.fillMaxWidth().testTag(RegistrationTestTags.FORM)) {
        AuthTextField(
            value = state.name,
            onValueChange = actions.onNameChange,
            label = stringResource(R.string.register_name_label),
            placeholder = stringResource(R.string.register_name_placeholder),
            helpText = null,
            errorMessage = when (state.nameError) {
                null, NameValidationResult.VALID -> null
                NameValidationResult.EMPTY -> stringResource(R.string.register_name_error_empty)
                NameValidationResult.TOO_SHORT -> stringResource(R.string.register_name_error_short)
            },
            readOnly = locked,
            onImeAction = {},
            onFocusLost = {},
            keyboardType = KeyboardType.Text,
            capitalization = KeyboardCapitalization.Words,
            imeAction = ImeAction.Next,
            autofillType = ContentType.PersonFullName,
            fieldTag = RegistrationTestTags.NAME_FIELD,
            errorTag = RegistrationTestTags.NAME_ERROR,
        )
        Spacer(Modifier.height(FieldGap))
        if (state.isPhoneEditable) {
            LoginPhoneField(
                value = state.formattedNumber,
                onValueChange = actions.onNumberChange,
                country = state.country,
                countries = CountryDialCodes.all,
                onCountryChange = actions.onCountryChange,
                label = stringResource(R.string.login_phone_label),
                countryLabel = stringResource(R.string.login_phone_country),
                helpText = "",
                errorMessage = when (state.numberError) {
                    null, PhoneValidationResult.VALID -> null
                    PhoneValidationResult.EMPTY -> stringResource(R.string.register_phone_error_empty)
                    // The prototype's message for India's ten-digit numbers; the previous app's for the others.
                    else -> stringResource(
                        if (state.country.dialCode == CountryDialCodes.INDIA.dialCode) R.string.login_phone_error_india else R.string.login_phone_error_invalid,
                    )
                },
                readOnly = locked,
                onImeAction = actions.onSubmit,
                onFocusLost = {},
            )
            Spacer(Modifier.height(FieldGap))
            ReadOnlyField(label = stringResource(R.string.login_email_label), value = state.email)
        } else {
            AuthTextField(
                value = state.email,
                onValueChange = actions.onEmailChange,
                label = stringResource(R.string.login_email_label),
                placeholder = stringResource(R.string.login_email_placeholder),
                helpText = null,
                errorMessage = when (state.emailError) {
                    null, EmailValidationResult.VALID -> null
                    EmailValidationResult.EMPTY -> stringResource(R.string.register_email_error_empty)
                    EmailValidationResult.INVALID_FORMAT -> stringResource(R.string.login_email_error_invalid)
                    EmailValidationResult.TOO_LONG -> stringResource(R.string.login_email_error_too_long)
                },
                readOnly = locked,
                onImeAction = actions.onSubmit,
                onFocusLost = {},
                fieldTag = RegistrationTestTags.EMAIL_FIELD,
                errorTag = RegistrationTestTags.EMAIL_ERROR,
            )
            Spacer(Modifier.height(FieldGap))
            ReadOnlyField(label = stringResource(R.string.login_phone_label), value = "${state.country.prefix} ${state.formattedNumber}")
        }
        if (state.offersReferral) {
            Spacer(Modifier.height(FieldGap))
            if (state.isReferralVisible) {
                AuthTextField(
                    value = state.referralCode,
                    onValueChange = actions.onReferralChange,
                    label = stringResource(R.string.register_referral_label),
                    placeholder = stringResource(R.string.register_referral_placeholder),
                    helpText = null,
                    errorMessage = null,
                    readOnly = locked,
                    onImeAction = actions.onSubmit,
                    onFocusLost = {},
                    keyboardType = KeyboardType.Ascii,
                    capitalization = KeyboardCapitalization.Characters,
                    autofillType = null,
                    fieldTag = RegistrationTestTags.REFERRAL_FIELD,
                )
            } else {
                Text(
                    text = stringResource(R.string.register_referral_toggle),
                    style = AppDesignSystem.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = AppDesignSystem.colors.primary,
                    modifier = Modifier
                        .testTag(RegistrationTestTags.REFERRAL_TOGGLE)
                        .minimumInteractiveComponentSize()
                        .clickable(enabled = !locked, role = Role.Button, onClick = actions.onShowReferral),
                )
            }
        }
    }

    state.error?.let { error ->
        Spacer(Modifier.height(ErrorTopSpacing))
        AuthInlineError(text = AuthMessage.textOf(error), modifier = Modifier.testTag(RegistrationTestTags.ERROR))
    }
    Spacer(Modifier.height(ButtonTopSpacing))
    LoginPrimaryButton(
        text = stringResource(R.string.register_submit),
        loading = state.isSubmitting,
        onClick = actions.onSubmit,
        modifier = Modifier.testTag(RegistrationTestTags.SUBMIT),
    )
}

/** What the account signed in with: shown in the field style, not editable (the previous app greys it out). */
@Composable
private fun ReadOnlyField(label: String, value: String) {
    AuthTextField(
        value = value,
        onValueChange = {},
        label = label,
        placeholder = "",
        helpText = null,
        errorMessage = null,
        readOnly = true,
        onImeAction = {},
        onFocusLost = {},
        keyboardType = KeyboardType.Text,
        autofillType = null,
        fieldTag = RegistrationTestTags.READ_ONLY,
    )
}
