package com.vplayed.core.ui.adaptive

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vplayed.core.designsystem.dimension.WindowWidthClass
import com.vplayed.core.designsystem.theme.AppDesignSystem

/**
 * Everything a screen needs to adapt to the current window: phone/tablet, portrait/landscape,
 * split-screen and freeform windows. Derived only from window size — never from device type or
 * orientation flags — so it is correct for resizable and multi-window use.
 */
@Immutable
data class AppLayoutInfo(
    val widthClass: WindowWidthClass,
    /** Height below 480dp, e.g. a phone in landscape. Prefer side navigation and compact headers. */
    val isHeightCompact: Boolean,
    val screenHorizontalPadding: Dp,
) {
    val isLandscape: Boolean get() = isHeightCompact || widthClass == WindowWidthClass.EXPANDED

    /** Navigation rail instead of a bottom bar whenever width allows or height is scarce. */
    val useNavigationRail: Boolean get() = widthClass != WindowWidthClass.COMPACT || isHeightCompact

    companion object {
        private const val MEDIUM_HEIGHT_DP = 480

        /** Pure mapping from window size (in dp) to layout decisions. [basePadding] is the compact padding. */
        fun forWindowSize(widthDp: Int, heightDp: Int, basePadding: Dp = 16.dp): AppLayoutInfo {
            val widthClass = WindowWidthClass.fromWidth(widthDp)
            val padding = when (widthClass) {
                WindowWidthClass.COMPACT -> basePadding
                WindowWidthClass.MEDIUM -> basePadding * 1.5f
                WindowWidthClass.EXPANDED -> basePadding * 2
            }
            return AppLayoutInfo(
                widthClass = widthClass,
                isHeightCompact = heightDp < MEDIUM_HEIGHT_DP,
                screenHorizontalPadding = padding,
            )
        }
    }
}

/** Current [AppLayoutInfo]; recomputed on rotation, resize, fold/unfold and multi-window changes. */
@Composable
fun rememberAppLayoutInfo(): AppLayoutInfo {
    val sizePx = LocalWindowInfo.current.containerSize
    val density = LocalDensity.current
    val basePadding = AppDesignSystem.dimens.screenHorizontalPadding
    return remember(sizePx, density, basePadding) {
        with(density) {
            AppLayoutInfo.forWindowSize(
                widthDp = sizePx.width.toDp().value.toInt(),
                heightDp = sizePx.height.toDp().value.toInt(),
                basePadding = basePadding,
            )
        }
    }
}

/**
 * Centers [content] and caps its width so lines stay readable on tablets and in landscape.
 * Use [maxWidth] = `readableContentMaxWidth` for forms/text, `contentMaxWidth` for grids.
 */
@Composable
fun ResponsiveContent(
    modifier: Modifier = Modifier,
    maxWidth: Dp = AppDesignSystem.dimens.contentMaxWidth,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Box(modifier = Modifier.widthIn(max = maxWidth).fillMaxWidth(), content = content)
    }
}
