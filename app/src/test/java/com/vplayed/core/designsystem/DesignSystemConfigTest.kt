package com.vplayed.core.designsystem

import com.vplayed.core.designsystem.color.BrandPalettes
import com.vplayed.core.designsystem.color.DefaultStatusColors
import com.vplayed.core.designsystem.color.extendedColorsFor
import com.vplayed.core.designsystem.dimension.AppDimensions
import com.vplayed.core.designsystem.dimension.UiDensity
import com.vplayed.core.designsystem.dimension.WindowWidthClass
import com.vplayed.core.designsystem.theme.AppearanceConfig
import com.vplayed.core.designsystem.theme.BrandConfig
import com.vplayed.core.designsystem.typography.AppFonts
import com.vplayed.core.designsystem.typography.TextScale
import com.vplayed.core.designsystem.typography.appTextStyles
import com.vplayed.core.designsystem.typography.appTypography
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DesignSystemConfigTest {

    @Test
    fun givenUnknownPaletteOrFontId_whenResolving_thenFallsBackToBrandDefaults() {
        assertEquals(BrandConfig.defaultPalette, BrandConfig.palette("removed-palette"))
        assertEquals(BrandConfig.defaultFont, BrandConfig.font("removed-font"))
    }

    @Test
    fun givenDefaultAppearance_whenResolving_thenUsesInterAndDefaultPalette() {
        val config = AppearanceConfig.Default
        assertEquals(AppFonts.Inter, BrandConfig.font(config.fontId))
        assertEquals(BrandPalettes.Vplayed, BrandConfig.palette(config.paletteId))
    }

    @Test
    fun givenEveryPalette_whenComparingModes_thenLightAndDarkBackgroundsDiffer() {
        BrandConfig.palettes.forEach { palette ->
            assertNotEquals(palette.id, palette.lightScheme.background, palette.darkScheme.background)
        }
    }

    @Test
    fun givenPaletteIds_whenChecking_thenTheyAreUnique() {
        val ids = BrandConfig.palettes.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun givenTextScale_whenBuildingTypography_thenSizesScaleWithinBounds() {
        val base = appTypography(AppFonts.InterFamily, TextScale.DEFAULT)
        val large = appTypography(AppFonts.InterFamily, 1.2f)
        val clamped = appTypography(AppFonts.InterFamily, 10f)

        assertEquals(base.bodyLarge.fontSize.value * 1.2f, large.bodyLarge.fontSize.value, 0.01f)
        assertEquals(base.bodyLarge.fontSize.value * TextScale.MAX, clamped.bodyLarge.fontSize.value, 0.01f)
    }

    @Test
    fun givenWiderWindow_whenBuildingOttStyles_thenHeadersGrowButBodyStylesDoNot() {
        val phone = appTextStyles(AppFonts.InterFamily, widthClass = WindowWidthClass.COMPACT)
        val tablet = appTextStyles(AppFonts.InterFamily, widthClass = WindowWidthClass.EXPANDED)

        assertTrue(tablet.heroTitle.fontSize.value > phone.heroTitle.fontSize.value)
        assertTrue(tablet.header.fontSize.value > phone.header.fontSize.value)
        assertTrue(tablet.subHeader.fontSize.value > phone.subHeader.fontSize.value)
        assertEquals(phone.cardTitle.fontSize, tablet.cardTitle.fontSize)
        assertEquals(phone.synopsis.fontSize, tablet.synopsis.fontSize)
    }

    @Test
    fun givenOttStyles_whenBuilding_thenHeaderHierarchyIsDescending() {
        val styles = appTextStyles(AppFonts.InterFamily)
        val sizes = listOf(styles.heroTitle, styles.screenHeader, styles.header, styles.subHeader, styles.cardTitle)
            .map { it.fontSize.value }
        assertEquals(sizes.sortedDescending(), sizes)
        assertEquals("tnum", styles.playerTimestamp.fontFeatureSettings)
    }

    @Test
    fun givenCompactDensity_whenResolvingDimensions_thenComponentsShrinkButTouchTargetDoesNot() {
        val comfortable = AppDimensions.forDensity(UiDensity.COMFORTABLE)
        val compact = AppDimensions.forDensity(UiDensity.COMPACT)

        assertTrue(compact.buttonHeight < comfortable.buttonHeight)
        assertEquals(comfortable.minTouchTarget, compact.minTouchTarget)
    }

    @Test
    fun givenScheme_whenDerivingExtendedColors_thenDividerFollowsOutlineVariant() {
        val scheme = BrandPalettes.Monochrome.darkScheme
        val extended = extendedColorsFor(scheme, DefaultStatusColors.dark)

        assertEquals(scheme.outlineVariant, extended.divider)
        assertEquals(DefaultStatusColors.dark.success, extended.success)
    }
}
