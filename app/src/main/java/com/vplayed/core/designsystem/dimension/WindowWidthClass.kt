package com.vplayed.core.designsystem.dimension

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo

/** Material 3 window width classes (compact < 600dp ≤ medium < 840dp ≤ expanded). */
enum class WindowWidthClass {
    COMPACT,
    MEDIUM,
    EXPANDED,
    ;

    companion object {
        const val MEDIUM_MIN_DP = 600
        const val EXPANDED_MIN_DP = 840

        fun fromWidth(widthDp: Int): WindowWidthClass = when {
            widthDp >= EXPANDED_MIN_DP -> EXPANDED
            widthDp >= MEDIUM_MIN_DP -> MEDIUM
            else -> COMPACT
        }
    }
}

/**
 * Width class of the current window. Updates on rotation, resize, fold/unfold and multi-window
 * changes (the activity handles those without being recreated).
 */
@Composable
fun currentWindowWidthClass(): WindowWidthClass {
    val widthPx = LocalWindowInfo.current.containerSize.width
    val widthDp = with(LocalDensity.current) { widthPx.toDp().value.toInt() }
    return WindowWidthClass.fromWidth(widthDp)
}
