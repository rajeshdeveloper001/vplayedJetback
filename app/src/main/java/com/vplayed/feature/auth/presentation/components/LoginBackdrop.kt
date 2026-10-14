package com.vplayed.feature.auth.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vplayed.core.designsystem.theme.AppDesignSystem

// Prototype `.bg-glow`: three soft radial glows (fixed px radii) placed by percentage.
private val PrimaryGlowRadius = 500.dp
private val SecondaryGlowRadius = 400.dp
private val AccentGlowRadius = 350.dp
private const val PRIMARY_GLOW_ALPHA = 0.12f
private const val SECONDARY_GLOW_ALPHA = 0.08f
private const val ACCENT_GLOW_ALPHA = 0.04f
private const val GLOW_FADE_STOP = 0.6f

/**
 * Sign-in backdrop (prototype `.bg-layer`): the auth canvas color with brand glows. The prototype's
 * hidden poster grid is not drawn; its dark overlay is kept implicit in the dark canvas.
 */
@Composable
internal fun Modifier.loginBackdrop(): Modifier {
    val auth = AppDesignSystem.extendedColors.auth
    val brand = AppDesignSystem.colors.primary
    val accent = AppDesignSystem.extendedColors.accentYellow
    return drawBehind {
        drawRect(auth.canvas)
        fun glow(color: Color, alpha: Float, radiusPx: Float, x: Float, y: Float) {
            val center = Offset(size.width * x, size.height * y)
            drawCircle(
                brush = Brush.radialGradient(
                    0f to color.copy(alpha = alpha),
                    GLOW_FADE_STOP to Color.Transparent,
                    center = center,
                    radius = radiusPx,
                ),
                radius = radiusPx,
                center = center,
            )
        }
        glow(brand, PRIMARY_GLOW_ALPHA, PrimaryGlowRadius.toPx(), 0.25f, 0.30f)
        glow(brand, SECONDARY_GLOW_ALPHA, SecondaryGlowRadius.toPx(), 0.75f, 0.70f)
        glow(accent, ACCENT_GLOW_ALPHA, AccentGlowRadius.toPx(), 0.60f, 0.20f)
    }
}
