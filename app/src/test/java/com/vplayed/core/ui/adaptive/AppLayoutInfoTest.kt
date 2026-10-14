package com.vplayed.core.ui.adaptive

import androidx.compose.ui.unit.dp
import com.vplayed.core.designsystem.dimension.WindowWidthClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLayoutInfoTest {

    @Test
    fun givenPhonePortrait_whenClassifying_thenCompactWithBottomBar() {
        val info = AppLayoutInfo.forWindowSize(widthDp = 411, heightDp = 891)
        assertEquals(WindowWidthClass.COMPACT, info.widthClass)
        assertFalse(info.useNavigationRail)
        assertFalse(info.isLandscape)
        assertEquals(16.dp, info.screenHorizontalPadding)
    }

    @Test
    fun givenPhoneLandscape_whenClassifying_thenHeightCompactWithRail() {
        val info = AppLayoutInfo.forWindowSize(widthDp = 891, heightDp = 411)
        assertTrue(info.isHeightCompact)
        assertTrue(info.isLandscape)
        assertTrue(info.useNavigationRail)
    }

    @Test
    fun givenFoldableUnfolded_whenClassifying_thenMediumWithRail() {
        val info = AppLayoutInfo.forWindowSize(widthDp = 673, heightDp = 841)
        assertEquals(WindowWidthClass.MEDIUM, info.widthClass)
        assertTrue(info.useNavigationRail)
        assertEquals(24.dp, info.screenHorizontalPadding)
    }

    @Test
    fun givenTablet_whenClassifying_thenExpandedWithWiderPadding() {
        val info = AppLayoutInfo.forWindowSize(widthDp = 1280, heightDp = 800)
        assertEquals(WindowWidthClass.EXPANDED, info.widthClass)
        assertEquals(32.dp, info.screenHorizontalPadding)
    }

    @Test
    fun givenBreakpointWidths_whenClassifying_thenBoundariesAreInclusive() {
        assertEquals(WindowWidthClass.COMPACT, WindowWidthClass.fromWidth(599))
        assertEquals(WindowWidthClass.MEDIUM, WindowWidthClass.fromWidth(600))
        assertEquals(WindowWidthClass.MEDIUM, WindowWidthClass.fromWidth(839))
        assertEquals(WindowWidthClass.EXPANDED, WindowWidthClass.fromWidth(840))
    }
}
