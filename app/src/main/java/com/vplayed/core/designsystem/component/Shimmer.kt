package com.vplayed.core.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import com.vplayed.core.designsystem.theme.AppDesignSystem

private const val SHIMMER_DURATION_MILLIS = 1200

/**
 * Animated skeleton fill (base color with a moving highlight band) using the theme's
 * `skeletonBase` / `skeletonHighlight` tokens, so it works in light and dark.
 */
fun Modifier.shimmer(shape: Shape? = null): Modifier = composed {
    val base = AppDesignSystem.extendedColors.skeletonBase
    val highlight = AppDesignSystem.extendedColors.skeletonHighlight
    val resolvedShape = shape ?: AppDesignSystem.shapes.small
    val progress by rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(SHIMMER_DURATION_MILLIS, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmerProgress",
    )
    drawWithCache {
        val outline = resolvedShape.createOutline(size, layoutDirection, this)
        onDrawBehind {
            val bandWidth = size.width.coerceAtLeast(size.height)
            val start = bandWidth * progress
            drawOutline(
                outline = outline,
                brush = Brush.linearGradient(
                    colors = listOf(base, highlight, base),
                    start = Offset(start - bandWidth, 0f),
                    end = Offset(start, size.height),
                ),
            )
        }
    }
}

/** A shimmering placeholder block. Size it with [modifier]. Hidden from accessibility services. */
@Composable
fun ShimmerBox(modifier: Modifier = Modifier, shape: Shape? = null) {
    Box(modifier.shimmer(shape))
}

/** Static (non-animated) skeleton fill, e.g. as an image placeholder while it downloads. */
fun Modifier.skeleton(shape: Shape? = null): Modifier = composed {
    background(AppDesignSystem.extendedColors.skeletonBase, shape ?: AppDesignSystem.shapes.small)
}
