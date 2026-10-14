package com.vplayed.feature.splashscreen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vplayed.core.designsystem.shape.AppMotion
import com.vplayed.core.designsystem.theme.AppTheme
import com.vplayed.feature.splashscreen.presentation.screen.SplashScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SplashScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun givenSplash_whenShown_thenDisplaysBrandLogo() {
        composeRule.setContent { AppTheme { SplashScreen(isReady = false, onSplashFinished = {}) } }
        composeRule.onNodeWithContentDescription("Vplayed").assertIsDisplayed()
    }

    @Test
    fun givenStartupNotReady_whenAnimationEnds_thenStaysOnSplash() {
        var finished = 0
        composeRule.setContent { AppTheme { SplashScreen(isReady = false, onSplashFinished = { finished++ }) } }

        composeRule.mainClock.advanceTimeBy(AppMotion.LONG_MILLIS * 2L)
        composeRule.waitForIdle()

        assertEquals(0, finished)
    }

    @Test
    fun givenStartupBecomesReady_whenAnimationEnds_thenFinishesExactlyOnce() {
        var finished = 0
        var ready by mutableStateOf(false)
        composeRule.setContent { AppTheme { SplashScreen(isReady = ready, onSplashFinished = { finished++ }) } }

        composeRule.mainClock.advanceTimeBy(AppMotion.LONG_MILLIS * 2L)
        ready = true
        composeRule.waitForIdle()

        assertEquals(1, finished)
    }
}
