package com.vplayed.core.designsystem.component

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/*
 * Brushes that reproduce CSS gradients from the prototype exactly, so components can copy the
 * prototype's values (angle, stops, color-mix) instead of approximating them.
 */

/**
 * CSS `linear-gradient(<angle>deg, …)`: 0° points up, 90° right; the gradient line spans the
 * box's projection on that direction, as in CSS.
 */
fun cssLinearGradient(angleDegrees: Float, vararg stops: Pair<Float, Color>): ShaderBrush = object : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        val radians = Math.toRadians(angleDegrees.toDouble())
        val dx = sin(radians).toFloat()
        val dy = -cos(radians).toFloat()
        val halfLength = (abs(size.width * dx) + abs(size.height * dy)) / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        return LinearGradientShader(
            from = center - Offset(dx, dy) * halfLength,
            to = center + Offset(dx, dy) * halfLength,
            colors = stops.map { it.second },
            colorStops = stops.map { it.first },
        )
    }
}

/**
 * CSS `radial-gradient(<rx%> <ry%> at <x%> <y%>, …)` as a circle stretched to an ellipse:
 * radii and center are fractions of the box size.
 */
fun cssRadialGradient(
    radiusX: Float,
    radiusY: Float,
    centerX: Float,
    centerY: Float,
    vararg stops: Pair<Float, Color>,
): ShaderBrush = object : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        val rx = size.width * radiusX
        val ry = size.height * radiusY
        val shader = RadialGradientShader(
            center = Offset(size.width * centerX, size.height * centerY),
            radius = rx,
            colors = stops.map { it.second },
            colorStops = stops.map { it.first },
        )
        // Scale vertically around the center to turn the circle into the CSS ellipse.
        val matrix = android.graphics.Matrix().apply { setScale(1f, ry / rx, size.width * centerX, size.height * centerY) }
        shader.setLocalMatrix(matrix)
        return shader
    }
}

/** CSS `color-mix(in srgb, [this] [fraction], [other])` with premultiplied alpha, as browsers compute it. */
fun Color.mixWith(other: Color, fraction: Float): Color {
    val alpha = alpha * fraction + other.alpha * (1 - fraction)
    if (alpha == 0f) return Color.Transparent
    val self = this
    fun channel(a: Float, b: Float) = (a * self.alpha * fraction + b * other.alpha * (1 - fraction)) / alpha
    return Color(
        red = channel(red, other.red),
        green = channel(green, other.green),
        blue = channel(blue, other.blue),
        alpha = alpha,
    )
}
