package com.vplayed.feature.auth.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vplayed.core.designsystem.theme.AppDesignSystem
import com.vplayed.core.designsystem.theme.AppTheme
import com.vplayed.core.ui.preview.ComponentPreviews

// Prototype input: 14/18 padding (and the neutral focus halo, see authFocusHalo). Label, input and help/error line
// are 8dp apart (prototype: 8 and 7).
private val FieldHorizontalPadding = 18.dp
private val FieldVerticalPadding = 14.dp

/**
 * Prototype email field (`.field`): label above a glass input, help text below that turns into
 * [errorMessage] when set. Calls [onFocusLost] when focus leaves the field.
 */
@Composable
internal fun LoginEmailField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
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
    val borderColor = when {
        errorMessage != null -> auth.fieldError
        focused -> auth.fieldBorderFocused
        else -> auth.fieldBorder
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(dimens.spacing.xs)) {
        Text(text = label, style = textStyles.formLabel, color = auth.textPrimary)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            readOnly = readOnly,
            singleLine = true,
            textStyle = textStyles.formInput.copy(color = auth.textStrong),
            cursorBrush = SolidColor(auth.textStrong),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { onImeAction() }),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(LoginTestTags.EMAIL_FIELD)
                .onFocusChanged { state ->
                    if (focused && !state.isFocused) onFocusLost()
                    focused = state.isFocused
                }
                .semantics {
                    contentType = ContentType.EmailAddress
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
                            text = placeholder,
                            style = textStyles.formInput.copy(fontWeight = FontWeight.Medium),
                            color = auth.textMuted,
                        )
                    }
                    innerTextField()
                }
            },
        )
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                style = textStyles.formSupporting.copy(fontWeight = FontWeight.SemiBold),
                color = AppDesignSystem.extendedColors.formAccent,
                modifier = Modifier
                    .testTag(LoginTestTags.EMAIL_ERROR)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )
        } else {
            Text(text = helpText, style = textStyles.formSupporting, color = auth.textSecondary)
        }
    }
}

@ComponentPreviews
@Composable
private fun LoginEmailFieldPreview() {
    AppTheme {
        Column(
            modifier = Modifier
                .background(AppDesignSystem.extendedColors.auth.canvas)
                .padding(AppDesignSystem.dimens.spacing.md),
            verticalArrangement = Arrangement.spacedBy(AppDesignSystem.dimens.spacing.md),
        ) {
            LoginEmailField("", {}, "Email ID", "vplayedproduct@gmail.com", "We'll send a 6-digit code.", null, false, {}, {})
            LoginEmailField("user@", {}, "Email ID", "", "", "Please enter a valid email address", false, {}, {})
        }
    }
}
