package com.vplayed.feature.auth.presentation.components

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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vplayed.core.designsystem.icon.AppIcons
import com.vplayed.core.designsystem.theme.AppDesignSystem
import com.vplayed.core.designsystem.theme.AppTheme
import com.vplayed.core.ui.preview.ComponentPreviews
import com.vplayed.shared.phone.CountryDialCode
import com.vplayed.shared.phone.CountryDialCodes
import java.util.Locale

// Same input metrics as LoginEmailField; the country chip is the prototype's `.vpl-cc` (14/10 padding, 8dp gap).
private val FieldHorizontalPadding = 18.dp
private val FieldVerticalPadding = 14.dp
private val ChipStartPadding = 14.dp
private val ChipEndPadding = 10.dp
private val ChipGap = 8.dp
private val ChipIconSize = 18.dp

/**
 * Prototype mobile-number field (`vp-login-method.js`): label above a country-code chip (opens the
 * country list) and the number input, help text below that turns into [errorMessage] when set.
 * [value] is shown as given (the shared state groups it `98765 43210`).
 */
@Composable
internal fun LoginPhoneField(
    value: String,
    onValueChange: (String) -> Unit,
    country: CountryDialCode,
    countries: List<CountryDialCode>,
    onCountryChange: (CountryDialCode) -> Unit,
    label: String,
    countryLabel: String,
    helpText: String,
    errorMessage: String?,
    readOnly: Boolean,
    onImeAction: () -> Unit,
    onFocusLost: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val auth = AppDesignSystem.extendedColors.auth
    val dimens = AppDesignSystem.dimens
    val textStyles = AppDesignSystem.textStyles
    val shape = AppDesignSystem.shapes.input
    var focused by remember { mutableStateOf(false) }
    var showCountries by remember { mutableStateOf(false) }
    val borderColor = when {
        errorMessage != null -> auth.fieldError
        focused -> auth.fieldBorderFocused
        else -> auth.fieldBorder
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(dimens.spacing.xs)) {
        Text(text = label, style = textStyles.formLabel, color = auth.textPrimary)
        Row(horizontalArrangement = Arrangement.spacedBy(ChipGap)) {
            Box {
                Row(
                    modifier = Modifier
                        .heightIn(min = dimens.formFieldMinHeight)
                        .background(auth.fieldFill, shape)
                        .border(dimens.borderThin, if (errorMessage != null) auth.fieldError else auth.fieldBorder, shape)
                        .clickable(enabled = !readOnly, role = Role.DropdownList) { showCountries = true }
                        .padding(start = ChipStartPadding, end = ChipEndPadding)
                        .semantics { contentDescription = "$countryLabel ${country.prefix}" },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(dimens.spacing.xs),
                ) {
                    Text(text = country.prefix, style = textStyles.formInput, color = auth.textStrong)
                    Icon(
                        painter = painterResource(AppIcons.ExpandMore),
                        contentDescription = null,
                        tint = auth.textMuted,
                        modifier = Modifier.size(ChipIconSize),
                    )
                }
                DropdownMenu(expanded = showCountries, onDismissRequest = { showCountries = false }) {
                    countries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text("${countryName(option)} (${option.prefix})") },
                            onClick = {
                                showCountries = false
                                onCountryChange(option)
                            },
                        )
                    }
                }
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                readOnly = readOnly,
                singleLine = true,
                textStyle = textStyles.formInput.copy(color = auth.textStrong),
                cursorBrush = SolidColor(auth.textStrong),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onImeAction() }),
                modifier = Modifier
                    .weight(1f)
                    .testTag(LoginTestTags.PHONE_FIELD)
                    .onFocusChanged { state ->
                        if (focused && !state.isFocused) onFocusLost()
                        focused = state.isFocused
                    }
                    .semantics {
                        contentType = ContentType.PhoneNumberNational
                        if (errorMessage != null) error(errorMessage)
                    },
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = dimens.formFieldMinHeight)
                            .authFocusHalo(visible = focused && errorMessage == null, cornerRadius = dimens.spacing.xs)
                            .background(auth.fieldFill, shape)
                            .border(dimens.borderThin, borderColor, shape)
                            .padding(horizontal = FieldHorizontalPadding, vertical = FieldVerticalPadding),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (value.isEmpty()) {
                            Text(
                                text = PLACEHOLDER,
                                style = textStyles.formInput.copy(fontWeight = FontWeight.Medium),
                                color = auth.textMuted,
                            )
                        }
                        innerTextField()
                    }
                },
            )
        }
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                style = textStyles.formSupporting.copy(fontWeight = FontWeight.SemiBold),
                color = AppDesignSystem.extendedColors.formAccent,
                modifier = Modifier
                    .testTag(LoginTestTags.PHONE_ERROR)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )
        } else if (helpText.isNotEmpty()) {
            Text(text = helpText, style = textStyles.formSupporting, color = auth.textSecondary)
        }
    }
}

/** The prototype's sample number. */
private const val PLACEHOLDER = "98765 43210"

/** The country's name in the device language, else the shared English name. */
private fun countryName(country: CountryDialCode): String =
    runCatching { Locale.Builder().setRegion(country.regionCode).build().displayCountry }.getOrNull()
        ?.takeIf { it.isNotBlank() && it != country.regionCode } ?: country.name

@ComponentPreviews
@Composable
private fun LoginPhoneFieldPreview() {
    AppTheme {
        Column(
            modifier = Modifier
                .background(AppDesignSystem.extendedColors.auth.canvas)
                .padding(AppDesignSystem.dimens.spacing.md),
            verticalArrangement = Arrangement.spacedBy(AppDesignSystem.dimens.spacing.md),
        ) {
            val india = CountryDialCodes.DEFAULT
            LoginPhoneField("", {}, india, CountryDialCodes.all, {}, "Mobile number", "Country code", "We'll text a 6-digit code.", null, false, {}, {})
            LoginPhoneField("98765 4321", {}, india, CountryDialCodes.all, {}, "Mobile number", "Country code", "", "Enter a valid 10-digit mobile number", false, {}, {})
        }
    }
}
