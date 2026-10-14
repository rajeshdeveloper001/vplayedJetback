package com.vplayed.core.designsystem.theme

import com.vplayed.core.designsystem.dimension.UiDensity
import com.vplayed.core.designsystem.typography.TextScale

/** Light/dark selection. [SYSTEM] follows the device setting and updates live when it changes. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * User-selectable appearance preferences. Immutable; persisted by `AppearanceRepository`, which is
 * the single source of truth. Ids refer to entries in [BrandConfig]; unknown ids (for example a
 * palette removed in a later release) fall back to the brand defaults when resolved.
 */
data class AppearanceConfig(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val paletteId: String = BrandConfig.defaultPalette.id,
    val fontId: String = BrandConfig.defaultFont.id,
    val textScale: Float = TextScale.DEFAULT,
    val density: UiDensity = UiDensity.COMFORTABLE,
    /** Use wallpaper-based colors on Android 12+. Ignored on older versions. */
    val dynamicColor: Boolean = BrandConfig.dynamicColorByDefault,
) {
    companion object {
        val Default = AppearanceConfig()
    }
}
