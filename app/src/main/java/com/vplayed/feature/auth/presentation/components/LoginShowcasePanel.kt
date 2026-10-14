package com.vplayed.feature.auth.presentation.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vplayed.R
import com.vplayed.core.designsystem.icon.AppIcons
import com.vplayed.core.designsystem.theme.AppDesignSystem
import com.vplayed.core.designsystem.theme.AppTheme
import com.vplayed.core.ui.preview.ComponentPreviews
import com.vplayed.core.ui.rememberReducedMotion

/** Width / height of the logo vector (viewport 500 × 95). */
private const val LOGO_ASPECT_RATIO = 500f / 95f
// Prototype `.left-heading`: 14dp under the title, 440dp max subtitle width; features 14dp apart.
private val TitleBottomSpacing = 14.dp
private val SubtitleMaxWidth = 440.dp
private val FeatureGap = 14.dp
private val FeatureIconCorner = 10.dp
private val FeatureIconSize = 20.dp
private val PlatformsTopPadding = 24.dp
private val PlatformsLabelSpacing = 18.dp
private const val PLATFORM_ALPHA = 0.7f
// Prototype `glow-sweep-smooth 6.5s`: a light band sweeps across the accent line.
private const val SWEEP_MILLIS = 6_500
private const val SWEEP_START = 2f
private const val SWEEP_END = -1f
private const val SWEEP_STATIC = 0.5f

private data class Feature(@DrawableRes val icon: Int, @StringRes val text: Int)

private val features = listOf(
    Feature(AppIcons.Devices, R.string.login_feature_devices),
    Feature(AppIcons.Views, R.string.login_feature_subscribe),
    Feature(AppIcons.Download, R.string.login_feature_download),
    Feature(AppIcons.FourK, R.string.login_feature_4k),
    Feature(AppIcons.Redeem, R.string.login_feature_coins),
)

private val platforms = listOf(
    R.string.login_platform_android_tv,
    R.string.login_platform_apple_tv,
    R.string.login_platform_samsung,
    R.string.login_platform_fire_tv,
    R.string.login_platform_lg,
)

/**
 * Left column of the wide sign-in layout (prototype `.left`): logo, headline with the sweeping
 * accent line, subtitle, feature list and "Also available on" platforms. The prototype's platform
 * logos are SVG wordmarks; they are shown here as their names.
 */
@Composable
internal fun LoginShowcasePanel(modifier: Modifier = Modifier) {
    val auth = AppDesignSystem.extendedColors.auth
    val textStyles = AppDesignSystem.textStyles
    val spacing = AppDesignSystem.dimens.spacing
    val logo = if (AppDesignSystem.isDark) R.drawable.ic_vplayed_logo_on_dark else R.drawable.ic_vplayed_logo_on_light

    Column(
        modifier = modifier.testTag(LoginTestTags.SHOWCASE),
        verticalArrangement = Arrangement.spacedBy(spacing.xxl - spacing.xxs),
    ) {
        Image(
            painter = painterResource(logo),
            contentDescription = stringResource(R.string.splash_logo_description),
            modifier = Modifier
                .height(AppDesignSystem.dimens.authLogoHeight)
                .aspectRatio(LOGO_ASPECT_RATIO),
        )
        Column {
            Text(text = stringResource(R.string.login_showcase_title_line1), style = textStyles.promoTitle, color = auth.textStrong)
            Text(
                text = stringResource(R.string.login_showcase_title_line2),
                style = textStyles.promoTitle.copy(brush = rememberSweepBrush()),
            )
            Text(
                text = stringResource(R.string.login_showcase_subtitle),
                style = textStyles.promoBody,
                color = auth.textSecondary,
                modifier = Modifier
                    .padding(top = TitleBottomSpacing)
                    .widthIn(max = SubtitleMaxWidth),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(FeatureGap)) {
            features.forEach { feature -> FeatureRow(feature) }
        }
        Column {
            HorizontalDivider(color = auth.iconTileFill, thickness = AppDesignSystem.dimens.dividerThickness)
            Text(
                text = stringResource(R.string.login_platforms_title).uppercase(),
                style = textStyles.badge,
                color = auth.textSecondary,
                modifier = Modifier.padding(top = PlatformsTopPadding, bottom = PlatformsLabelSpacing),
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(PlatformsLabelSpacing),
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                platforms.forEach { name ->
                    Text(
                        text = stringResource(name),
                        style = textStyles.subHeader,
                        color = auth.textPrimary.copy(alpha = PLATFORM_ALPHA),
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureRow(feature: Feature) {
    val auth = AppDesignSystem.extendedColors.auth
    val dimens = AppDesignSystem.dimens
    val tileShape = RoundedCornerShape(FeatureIconCorner)
    Row(
        modifier = Modifier.padding(vertical = dimens.spacing.xxs),
        horizontalArrangement = Arrangement.spacedBy(FeatureGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(dimens.authIconButtonSize)
                .background(auth.iconTileFill, tileShape)
                .border(dimens.borderThin, auth.iconTileBorder, tileShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(feature.icon),
                contentDescription = null,
                tint = auth.textSecondary,
                modifier = Modifier.size(FeatureIconSize),
            )
        }
        Text(text = stringResource(feature.text), style = AppDesignSystem.textStyles.promoBody, color = auth.textPrimary)
    }
}

/**
 * Prototype `.accent`: brand text with a lighter band (`background-size: 200%`, repeating) whose
 * position moves from 200% to -100%. Still when the user turned animations off.
 */
@Composable
private fun rememberSweepBrush(): ShaderBrush {
    val brand = AppDesignSystem.colors.primary
    val highlight = AppDesignSystem.colors.onPrimaryContainer
    val position = if (rememberReducedMotion()) {
        SWEEP_STATIC
    } else {
        val transition = rememberInfiniteTransition(label = "loginSweep")
        val animated by transition.animateFloat(
            initialValue = SWEEP_START,
            targetValue = SWEEP_END,
            animationSpec = infiniteRepeatable(tween(SWEEP_MILLIS, easing = LinearEasing), RepeatMode.Restart),
            label = "loginSweepPosition",
        )
        animated
    }
    return remember(brand, highlight, position) { SweepBrush(brand, highlight, position) }
}

private class SweepBrush(
    private val brand: Color,
    private val highlight: Color,
    private val position: Float,
) : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        val bandWidth = size.width * 2
        // CSS background-position X% with a 2× wide image: offset = (width - 2·width) · X.
        val start = -size.width * position
        return LinearGradientShader(
            from = Offset(start, 0f),
            to = Offset(start + bandWidth, 0f),
            colors = listOf(brand, brand, highlight, brand, brand),
            colorStops = listOf(0f, 0.42f, 0.5f, 0.58f, 1f),
            tileMode = TileMode.Repeated,
        )
    }
}

@ComponentPreviews
@Composable
private fun LoginShowcasePanelPreview() {
    AppTheme {
        LoginShowcasePanel(
            modifier = Modifier
                .background(AppDesignSystem.extendedColors.auth.canvas)
                .padding(AppDesignSystem.dimens.spacing.huge)
                .fillMaxWidth(),
        )
    }
}
