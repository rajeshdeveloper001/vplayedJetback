package com.vplayed.core.designsystem.shape

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Corner shapes. The first five feed Material's [Shapes]; the rest are per-component. */
@Immutable
data class AppShapes(
    val extraSmall: RoundedCornerShape = RoundedCornerShape(4.dp),
    val small: RoundedCornerShape = RoundedCornerShape(8.dp),
    val medium: RoundedCornerShape = RoundedCornerShape(12.dp),
    val large: RoundedCornerShape = RoundedCornerShape(16.dp),
    val extraLarge: RoundedCornerShape = RoundedCornerShape(28.dp),
    /** Prototype cards and posters use 8dp corners. */
    val card: Shape = small,
    /** Prototype buttons: 8dp corners. */
    val button: Shape = small,
    val input: Shape = small,
    val dialog: Shape = extraLarge,
    val bottomSheet: Shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    val chip: Shape = small,
    /** Prototype live cards and channel tiles: 12dp. */
    val tile: Shape = medium,
    /** Prototype logo wells and icon tiles: 16dp. */
    val panel: Shape = large,
    /** Prototype bulk-purchase promo card (`.premium-in`): 24dp. */
    val promo: Shape = RoundedCornerShape(24.dp),
) {
    fun toMaterialShapes(): Shapes = Shapes(
        extraSmall = extraSmall,
        small = small,
        medium = medium,
        large = large,
        extraLarge = extraLarge,
    )
}

/** Tonal elevation levels (Material 3 levels 0–5). */
@Immutable
data class AppElevations(
    val level0: Dp = 0.dp,
    val level1: Dp = 1.dp,
    val level2: Dp = 3.dp,
    val level3: Dp = 6.dp,
    val level4: Dp = 8.dp,
    val level5: Dp = 12.dp,
    val card: Dp = level1,
    val dialog: Dp = level3,
    val toolbar: Dp = level0,
    val navigation: Dp = level2,
)

/** Standard motion. Use these specs instead of ad-hoc tween durations. */
object AppMotion {
    const val SHORT_MILLIS = 150
    const val MEDIUM_MILLIS = 300
    const val LONG_MILLIS = 500

    fun <T> standard(durationMillis: Int = MEDIUM_MILLIS): FiniteAnimationSpec<T> =
        tween(durationMillis = durationMillis, easing = FastOutSlowInEasing)
}
