package com.vplayed.feature.auth.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vplayed.core.designsystem.theme.AppDesignSystem

private val HaloWidth = 4.dp

/**
 * Prototype focus halo: a 4dp neutral ring around a focused field (`box-shadow: 0 0 0 4px`). Fields
 * never get a red focus. Drawn outside the bounds, so leave room around the field.
 */
@Composable
internal fun Modifier.authFocusHalo(visible: Boolean, cornerRadius: Dp): Modifier {
    if (!visible) return this
    val color = AppDesignSystem.extendedColors.auth.focusRing
    return drawBehind {
        val ring = HaloWidth.toPx()
        val radius = cornerRadius.toPx() + ring
        drawRoundRect(
            color = color,
            topLeft = Offset(-ring, -ring),
            size = Size(size.width + ring * 2, size.height + ring * 2),
            cornerRadius = CornerRadius(radius, radius),
        )
    }
}
