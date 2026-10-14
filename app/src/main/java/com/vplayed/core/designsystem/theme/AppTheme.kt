package com.vplayed.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import com.vplayed.core.designsystem.color.AppExtendedColors
import com.vplayed.core.designsystem.color.extendedColorsFor
import com.vplayed.core.designsystem.dimension.AppDimensions
import com.vplayed.core.designsystem.dimension.WindowWidthClass
import com.vplayed.core.designsystem.dimension.currentWindowWidthClass
import com.vplayed.core.designsystem.shape.AppElevations
import com.vplayed.core.designsystem.shape.AppShapes
import com.vplayed.core.designsystem.typography.AppTextStyles
import com.vplayed.core.designsystem.typography.appTextStyles
import com.vplayed.core.designsystem.typography.appTypography

private val LocalAppExtendedColors = staticCompositionLocalOf<AppExtendedColors> {
    error("AppExtendedColors not provided. Wrap content in AppTheme.")
}
private val LocalAppTextStyles = staticCompositionLocalOf<AppTextStyles> {
    error("AppTextStyles not provided. Wrap content in AppTheme.")
}
private val LocalAppIsDark = staticCompositionLocalOf { false }
private val LocalAppDimensions = staticCompositionLocalOf { AppDimensions.Comfortable }
private val LocalAppShapes = staticCompositionLocalOf { AppShapes() }
private val LocalAppElevations = staticCompositionLocalOf { AppElevations() }

/** Resolves [ThemeMode] against the device setting. Recomposes when the system mode changes. */
@Composable
@ReadOnlyComposable
fun ThemeMode.isDark(): Boolean = when (this) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

/**
 * The single application theme. Every screen, component and preview must be wrapped in it.
 *
 * @param appearance user preferences (from `AppearanceRepository` at runtime; defaults in previews).
 * @param darkTheme resolved light/dark mode; override only in previews and tests.
 * @param windowWidthClass drives header text sizes; follows the window automatically.
 */
@Composable
fun AppTheme(
    appearance: AppearanceConfig = AppearanceConfig.Default,
    darkTheme: Boolean = appearance.themeMode.isDark(),
    windowWidthClass: WindowWidthClass = currentWindowWidthClass(),
    content: @Composable () -> Unit,
) {
    val palette = BrandConfig.palette(appearance.paletteId)
    val useDynamic = appearance.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val context = LocalContext.current
    val colorScheme: ColorScheme = when {
        useDynamic && darkTheme -> dynamicDarkColorScheme(context)
        useDynamic -> dynamicLightColorScheme(context)
        darkTheme -> palette.darkScheme
        else -> palette.lightScheme
    }
    val status = if (darkTheme) palette.darkStatus else palette.lightStatus
    val extendedColors = remember(colorScheme, status, darkTheme) { extendedColorsFor(colorScheme, status, darkTheme) }

    val fontFamily = BrandConfig.font(appearance.fontId).fontFamily
    val typography = remember(fontFamily, appearance.textScale) {
        appTypography(fontFamily, appearance.textScale)
    }
    val textStyles = remember(fontFamily, appearance.textScale, windowWidthClass) {
        appTextStyles(fontFamily, appearance.textScale, windowWidthClass)
    }
    val dimensions = AppDimensions.forDensity(appearance.density)
    val shapes = remember { AppShapes() }

    CompositionLocalProvider(
        LocalAppExtendedColors provides extendedColors,
        LocalAppIsDark provides darkTheme,
        LocalAppTextStyles provides textStyles,
        LocalAppDimensions provides dimensions,
        LocalAppShapes provides shapes,
        LocalAppElevations provides AppElevations(),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = shapes.toMaterialShapes(),
            content = content,
        )
    }
}

/**
 * Single entry point for design tokens inside composables, e.g.
 * `AppDesignSystem.colors.primary`, `AppDesignSystem.dimens.spacing.md`.
 */
object AppDesignSystem {
    /**
     * Whether the resolved app theme is dark (user override or system). Use it to pick
     * theme-specific assets such as the logo; `isSystemInDarkTheme()` ignores the user override.
     */
    val isDark: Boolean
        @Composable @ReadOnlyComposable get() = LocalAppIsDark.current

    /** Material color roles: primary, onPrimary, surface, outline, error, … */
    val colors: ColorScheme
        @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme

    /** success, warning, info, disabled, divider, overlay. */
    val extendedColors: AppExtendedColors
        @Composable @ReadOnlyComposable get() = LocalAppExtendedColors.current

    /** Material type scale; used by Material components and general UI text. */
    val typography: Typography
        @Composable @ReadOnlyComposable get() = MaterialTheme.typography

    /** OTT roles: heroTitle, screenHeader, header, subHeader, cardTitle, metadata, badge, … */
    val textStyles: AppTextStyles
        @Composable @ReadOnlyComposable get() = LocalAppTextStyles.current

    val dimens: AppDimensions
        @Composable @ReadOnlyComposable get() = LocalAppDimensions.current

    val shapes: AppShapes
        @Composable @ReadOnlyComposable get() = LocalAppShapes.current

    val elevations: AppElevations
        @Composable @ReadOnlyComposable get() = LocalAppElevations.current
}
