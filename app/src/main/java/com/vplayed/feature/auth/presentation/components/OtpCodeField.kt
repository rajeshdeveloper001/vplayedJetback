package com.vplayed.feature.auth.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vplayed.core.designsystem.theme.AppDesignSystem
import com.vplayed.core.designsystem.theme.AppTheme
import com.vplayed.core.ui.preview.ComponentPreviews
import com.vplayed.shared.l10n.Res
import com.vplayed.shared.l10n.otp_code_description
import org.jetbrains.compose.resources.stringResource

/** Up to this window width the boxes use the prototype's ≤ 400px sizes (46dp boxes, 7dp apart). */
private val NarrowWindowWidth = 400.dp

/**
 * Verification-code input (prototype `.otp-row`): [length] square boxes driven by ONE text field,
 * so typing, deleting, pasting (digits only), autofill from an SMS and the number keyboard all work
 * natively, and the box that receives the next digit is highlighted. The field itself is invisible
 * and sits over the boxes. The keyboard opens when the field is first shown, as the prototype
 * focuses its first box.
 */
@Composable
internal fun OtpCodeField(
    code: String,
    length: Int,
    errorText: String?,
    enabled: Boolean,
    onCodeChange: (String) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = AppDesignSystem.dimens
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    var focused by remember { mutableStateOf(false) }
    val description = stringResource(Res.string.otp_code_description)

    // Focus when shown and again when input is re-enabled (after a refused code the prototype
    // empties the boxes and focuses the first one).
    LaunchedEffect(enabled) {
        if (!enabled) return@LaunchedEffect
        try {
            focusRequester.requestFocus()
        } catch (_: IllegalStateException) {
            // Not attached yet (e.g. the screen left composition at once); the user can tap the boxes.
        }
    }

    // The window width, not BoxWithConstraints: this field sits in a layout that is measured with
    // intrinsic sizes (the two-column modal), which subcomposing layouts do not support.
    val windowWidth = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() }
    val narrow = windowWidth <= NarrowWindowWidth
    val gap = if (narrow) dimens.otpBoxGapNarrow else dimens.otpBoxGap
    val boxMax = if (narrow) dimens.otpBoxMaxSizeNarrow else dimens.otpBoxMaxSize
    Box(modifier = modifier.fillMaxWidth()) {
        BasicTextField(
            value = code,
            onValueChange = onCodeChange,
            enabled = enabled,
            singleLine = true,
            textStyle = TextStyle(color = Color.Transparent),
            cursorBrush = SolidColor(Color.Transparent),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
                autoCorrectEnabled = false,
            ),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(OtpTestTags.CODE_FIELD)
                .focusRequester(focusRequester)
                .onFocusChanged { focused = it.isFocused }
                .semantics {
                    contentType = ContentType.SmsOtpCode
                    contentDescription = description
                    if (errorText != null) error(errorText)
                },
            decorationBox = { innerTextField ->
                Box {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(enabled) {
                                detectTapGestures {
                                    if (enabled) {
                                        focusRequester.requestFocus()
                                        keyboard?.show()
                                    }
                                }
                            },
                        horizontalArrangement = Arrangement.spacedBy(gap, Alignment.Start),
                    ) {
                        repeat(length) { index ->
                            OtpBox(
                                digit = code.getOrNull(index),
                                active = enabled && focused && index == code.length.coerceAtMost(length - 1),
                                modifier = Modifier.weight(1f, fill = false).widthIn(max = boxMax),
                            )
                        }
                    }
                    // The real (invisible) field: it still receives focus, taps, selection and paste.
                    Box(modifier = Modifier.matchParentSize().alpha(0f)) { innerTextField() }
                }
            },
        )
    }
}

@Composable
private fun OtpBox(digit: Char?, active: Boolean, modifier: Modifier = Modifier) {
    val auth = AppDesignSystem.extendedColors.auth
    val shape = AppDesignSystem.shapes.input
    val borderColor = when {
        active -> auth.fieldBorderFocused
        digit != null -> auth.fieldBorder
        else -> auth.socialBorder
    }
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .authFocusHalo(visible = active, cornerRadius = AppDesignSystem.dimens.spacing.xs)
            .background(auth.fieldFill, shape)
            .border(AppDesignSystem.dimens.borderThin, borderColor, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (digit != null) {
            Text(
                text = digit.toString(),
                style = AppDesignSystem.textStyles.formCode,
                color = auth.textStrong,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@ComponentPreviews
@Composable
private fun OtpCodeFieldPreview() {
    AppTheme {
        Column(
            modifier = Modifier
                .background(AppDesignSystem.extendedColors.auth.canvas)
                .padding(AppDesignSystem.dimens.spacing.md),
            verticalArrangement = Arrangement.spacedBy(AppDesignSystem.dimens.spacing.md),
        ) {
            OtpCodeField("12", 6, errorText = null, enabled = true, onCodeChange = {}, onDone = {})
            OtpCodeField("", 6, errorText = "Incorrect code. Try again.", enabled = true, onCodeChange = {}, onDone = {})
        }
    }
}
