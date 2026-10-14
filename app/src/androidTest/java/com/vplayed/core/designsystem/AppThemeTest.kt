package com.vplayed.core.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Typography
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vplayed.core.designsystem.color.BrandPalettes
import com.vplayed.core.designsystem.dimension.AppDimensions
import com.vplayed.core.designsystem.dimension.UiDensity
import com.vplayed.core.designsystem.dimension.WindowWidthClass
import com.vplayed.core.designsystem.theme.AppDesignSystem
import com.vplayed.core.designsystem.theme.AppTheme
import com.vplayed.core.designsystem.theme.AppearanceConfig
import com.vplayed.core.designsystem.theme.ThemeMode
import com.vplayed.core.designsystem.typography.AppFonts
import com.vplayed.core.designsystem.typography.AppTextStyles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Verifies that AppTheme turns an AppearanceConfig into the expected design tokens. */
@RunWith(AndroidJUnit4::class)
class AppThemeTest {

    @get:Rule
    val composeRule = createComposeRule()

    private class Captured {
        lateinit var colors: ColorScheme
        lateinit var typography: Typography
        lateinit var textStyles: AppTextStyles
        lateinit var dimens: AppDimensions
    }

    private fun capture(
        appearance: AppearanceConfig,
        darkTheme: Boolean? = null,
        widthClass: WindowWidthClass = WindowWidthClass.COMPACT,
    ): Captured {
        val captured = Captured()
        composeRule.setContent {
            val content: @androidx.compose.runtime.Composable () -> Unit = {
                captured.colors = AppDesignSystem.colors
                captured.typography = AppDesignSystem.typography
                captured.textStyles = AppDesignSystem.textStyles
                captured.dimens = AppDesignSystem.dimens
            }
            if (darkTheme == null) {
                AppTheme(appearance = appearance, windowWidthClass = widthClass, content = content)
            } else {
                AppTheme(appearance = appearance, darkTheme = darkTheme, windowWidthClass = widthClass, content = content)
            }
        }
        composeRule.waitForIdle()
        return captured
    }

    @Test
    fun givenLightTheme_whenRendering_thenUsesPaletteLightScheme() {
        val captured = capture(AppearanceConfig.Default, darkTheme = false)
        assertEquals(BrandPalettes.Vplayed.lightScheme.background, captured.colors.background)
        assertEquals(BrandPalettes.Vplayed.lightScheme.primary, captured.colors.primary)
    }

    @Test
    fun givenDarkTheme_whenRendering_thenUsesPaletteDarkScheme() {
        val captured = capture(AppearanceConfig.Default, darkTheme = true)
        assertEquals(BrandPalettes.Vplayed.darkScheme.background, captured.colors.background)
    }

    @Test
    fun givenForcedDarkMode_whenRendering_thenIgnoresSystemSetting() {
        val captured = capture(AppearanceConfig(themeMode = ThemeMode.DARK))
        assertEquals(BrandPalettes.Vplayed.darkScheme.surface, captured.colors.surface)
    }

    @Test
    fun givenForcedLightMode_whenRendering_thenIgnoresSystemSetting() {
        val captured = capture(AppearanceConfig(themeMode = ThemeMode.LIGHT))
        assertEquals(BrandPalettes.Vplayed.lightScheme.surface, captured.colors.surface)
    }

    @Test
    fun givenMonochromePalette_whenRendering_thenPrimaryComesFromMonochrome() {
        val captured = capture(AppearanceConfig(paletteId = BrandPalettes.Monochrome.id), darkTheme = false)
        assertEquals(BrandPalettes.Monochrome.lightScheme.primary, captured.colors.primary)
    }

    @Test
    fun givenDefaultAppearance_whenRendering_thenAllTextUsesInter() {
        val captured = capture(AppearanceConfig.Default, darkTheme = false)
        assertEquals(AppFonts.InterFamily, captured.typography.bodyLarge.fontFamily)
        assertEquals(AppFonts.InterFamily, captured.textStyles.header.fontFamily)
    }

    @Test
    fun givenLargerTextScale_whenRendering_thenTypographyScales() {
        val captured = capture(AppearanceConfig(textScale = 1.2f), darkTheme = false)
        assertEquals(16f * 1.2f, captured.typography.bodyLarge.fontSize.value, 0.01f)
    }

    @Test
    fun givenExpandedWindow_whenRendering_thenHeadersAreLargerThanOnPhones() {
        val tablet = capture(AppearanceConfig.Default, darkTheme = false, widthClass = WindowWidthClass.EXPANDED)
        assertTrue(tablet.textStyles.header.fontSize.value > 20f)
        assertEquals(14f, tablet.textStyles.cardTitle.fontSize.value, 0.01f)
    }

    @Test
    fun givenCompactDensity_whenRendering_thenProvidesCompactDimensions() {
        val captured = capture(AppearanceConfig(density = UiDensity.COMPACT), darkTheme = false)
        assertEquals(AppDimensions.Compact, captured.dimens)
    }
}
