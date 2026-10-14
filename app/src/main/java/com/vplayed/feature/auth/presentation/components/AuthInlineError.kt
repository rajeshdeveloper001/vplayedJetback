package com.vplayed.feature.auth.presentation.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.vplayed.core.designsystem.theme.AppDesignSystem

/**
 * Inline error line of the verification screens (prototype `.wpm-err`: 12px semibold, danger red).
 * Announced to screen readers when it appears. There are no Toasts on these screens.
 */
@Composable
internal fun AuthInlineError(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = AppDesignSystem.textStyles.formSupporting.copy(fontWeight = FontWeight.SemiBold),
        color = AppDesignSystem.extendedColors.formAccent,
        modifier = modifier
            .testTag(OtpTestTags.ERROR)
            .semantics { liveRegion = LiveRegionMode.Polite },
    )
}
