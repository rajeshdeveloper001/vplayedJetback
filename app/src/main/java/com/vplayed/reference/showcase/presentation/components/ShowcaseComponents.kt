package com.vplayed.reference.showcase.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.vplayed.core.designsystem.theme.AppDesignSystem

/** Section heading inside showcase cards; exposed as a heading for screen-reader navigation. */
@Composable
internal fun SectionTitle(text: String) {
    Text(
        text = text,
        style = AppDesignSystem.typography.titleMedium,
        color = AppDesignSystem.colors.primary,
        modifier = Modifier
            .padding(bottom = AppDesignSystem.dimens.spacing.xs)
            .semantics { heading() },
    )
}

/** A labelled chip filled with a theme color, drawn with its matching "on" color. */
@Composable
internal fun ColorSwatch(name: String, color: Color, onColor: Color) {
    val dimens = AppDesignSystem.dimens
    Box(
        modifier = Modifier
            .heightIn(min = dimens.minTouchTarget)
            .background(color, AppDesignSystem.shapes.small)
            .border(dimens.borderThin, AppDesignSystem.extendedColors.divider, AppDesignSystem.shapes.small)
            .padding(horizontal = dimens.spacing.sm, vertical = dimens.spacing.xs),
        contentAlignment = Alignment.Center,
    ) {
        Text(name, style = AppDesignSystem.typography.labelMedium, color = onColor)
    }
}
