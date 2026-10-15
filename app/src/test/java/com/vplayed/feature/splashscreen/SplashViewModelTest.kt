package com.vplayed.feature.splashscreen

import com.vplayed.core.designsystem.theme.AppearanceConfig
import com.vplayed.core.repository.AppearanceRepository
import com.vplayed.feature.splashscreen.di.SplashConfig
import com.vplayed.feature.splashscreen.di.SplashModule
import com.vplayed.feature.splashscreen.presentation.viewmodel.SplashUiState
import com.vplayed.feature.splashscreen.presentation.viewmodel.SplashViewModel
import com.vplayed.testing.FakeAppearanceRepository
import com.vplayed.testing.MainDispatcherRule
import com.vplayed.testing.testSessionManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {

    // Standard dispatcher: virtual time only moves when the test advances it.
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    private val threeSeconds = SplashConfig(minDisplayMillis = SplashModule.MIN_DISPLAY_MILLIS)

    @Test
    fun givenFastStartup_whenLessThanThreeSecondsPassed_thenStaysOnSplash() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = SplashViewModel(FakeAppearanceRepository(), testSessionManager(), threeSeconds)

        advanceTimeBy(SplashModule.MIN_DISPLAY_MILLIS - 1)
        runCurrent()

        assertEquals(SplashUiState.Starting, viewModel.uiState.value)
    }

    @Test
    fun givenFastStartup_whenThreeSecondsPassed_thenBecomesReady() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = SplashViewModel(FakeAppearanceRepository(), testSessionManager(), threeSeconds)

        advanceTimeBy(SplashModule.MIN_DISPLAY_MILLIS + 1)

        assertEquals(SplashUiState.Ready, viewModel.uiState.value)
    }

    @Test
    fun givenPreferencesNotYetLoaded_whenMinimumTimePassed_thenWaitsForStartupWork() = runTest(mainDispatcherRule.testDispatcher) {
        val pending = MutableSharedFlow<AppearanceConfig>(replay = 1)
        val repository = object : AppearanceRepository by FakeAppearanceRepository() {
            override val appearance: Flow<AppearanceConfig> = pending
        }
        val viewModel = SplashViewModel(repository, testSessionManager(), threeSeconds)

        advanceTimeBy(SplashModule.MIN_DISPLAY_MILLIS + 1)
        assertEquals(SplashUiState.Starting, viewModel.uiState.value)

        pending.tryEmit(AppearanceConfig.Default)
        runCurrent()
        assertEquals(SplashUiState.Ready, viewModel.uiState.value)
    }

    @Test
    fun givenAppDefaults_whenReadingSplashDuration_thenItIsThreeSeconds() {
        assertEquals(3_000L, SplashModule.MIN_DISPLAY_MILLIS)
    }
}
