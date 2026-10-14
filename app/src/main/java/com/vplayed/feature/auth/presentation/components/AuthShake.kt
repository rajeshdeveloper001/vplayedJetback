package com.vplayed.feature.auth.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp
import com.vplayed.core.ui.rememberReducedMotion

// Prototype `wpShake` (0.4s): 0 → -8 → +7 → -4 → +2 → 0 px at 20% steps.
private const val SHAKE_MILLIS = 400
private val ShakeOffsets = listOf(-8, 7, -4, 2, 0)

/**
 * Shakes its content sideways once each time [trigger] changes (prototype `.shake`, used when a
 * code is refused). The first value does not shake, and nothing moves when the user turned
 * animations off. The offset only shifts drawing, so layout is not disturbed.
 */
@Composable
internal fun Modifier.shakeOnChange(trigger: Int): Modifier {
    val offset = remember { Animatable(0f) }
    val reducedMotion = rememberReducedMotion()
    val initial = remember { trigger }
    LaunchedEffect(trigger) {
        if (trigger == initial || reducedMotion) return@LaunchedEffect
        offset.snapTo(0f)
        offset.animateTo(
            targetValue = 0f,
            animationSpec = keyframes {
                durationMillis = SHAKE_MILLIS
                ShakeOffsets.forEachIndexed { index, dp -> dp.toFloat() at (index + 1) * SHAKE_MILLIS / ShakeOffsets.size }
            },
        )
    }
    return this.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) {
            placeable.placeRelative(offset.value.dp.roundToPx(), 0)
        }
    }
}
