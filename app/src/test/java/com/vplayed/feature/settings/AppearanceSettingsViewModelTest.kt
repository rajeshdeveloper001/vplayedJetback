package com.vplayed.feature.settings

import com.vplayed.core.designsystem.dimension.UiDensity
import com.vplayed.core.designsystem.theme.AppearanceConfig
import com.vplayed.core.designsystem.theme.ThemeMode
import com.vplayed.core.ui.base.AppearanceUiState
import com.vplayed.core.ui.base.AppearanceViewModel
import com.vplayed.feature.settings.presentation.viewmodel.AppearanceSettingsUiState
import com.vplayed.feature.settings.presentation.viewmodel.AppearanceSettingsViewModel
import com.vplayed.testing.FakeAppearanceRepository
import com.vplayed.testing.MainDispatcherRule
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AppearanceSettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeAppearanceRepository()

    // Created in @Before: viewModelScope needs the test Main dispatcher installed by the rule.
    private lateinit var viewModel: AppearanceSettingsViewModel

    @Before
    fun setUp() {
        viewModel = AppearanceSettingsViewModel(repository)
    }

    private fun AppearanceSettingsViewModel.readyConfig(): AppearanceConfig =
        (uiState.value as AppearanceSettingsUiState.Ready).appearance

    @Test
    fun givenNoSubscriber_whenCreated_thenStateIsLoading() {
        assertEquals(AppearanceSettingsUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun givenSubscriber_whenRepositoryEmits_thenStateIsReady() = runTest {
        backgroundScope.launch(mainDispatcherRule.testDispatcher) { viewModel.uiState.collect() }
        assertEquals(AppearanceSettingsUiState.Ready(AppearanceConfig.Default), viewModel.uiState.value)
    }

    @Test
    fun givenUserSelections_whenApplied_thenRepositoryIsSingleSourceOfTruth() = runTest {
        backgroundScope.launch(mainDispatcherRule.testDispatcher) { viewModel.uiState.collect() }

        viewModel.onThemeModeSelected(ThemeMode.DARK)
        viewModel.onPaletteSelected("monochrome")
        viewModel.onDensitySelected(UiDensity.COMPACT)
        viewModel.onTextScaleChanged(1.15f)

        val expected = AppearanceConfig(
            themeMode = ThemeMode.DARK,
            paletteId = "monochrome",
            density = UiDensity.COMPACT,
            textScale = 1.15f,
        )
        assertEquals(expected, repository.state.value)
        assertEquals(expected, viewModel.readyConfig())
    }

    @Test
    fun givenCustomizedAppearance_whenResetClicked_thenDefaultsRestored() = runTest {
        backgroundScope.launch(mainDispatcherRule.testDispatcher) { viewModel.uiState.collect() }
        viewModel.onThemeModeSelected(ThemeMode.LIGHT)

        viewModel.onResetClicked()

        assertEquals(AppearanceConfig.Default, viewModel.readyConfig())
    }

    @Test
    fun givenThemeChangedInSettings_whenObservedByAppearanceViewModel_thenAppThemeUpdates() = runTest {
        val appearanceViewModel = AppearanceViewModel(repository)
        viewModel.onThemeModeSelected(ThemeMode.DARK)
        assertEquals(AppearanceUiState.Ready(repository.state.value), appearanceViewModel.uiState.value)
        assertEquals(ThemeMode.DARK, (appearanceViewModel.uiState.value as AppearanceUiState.Ready).appearance.themeMode)
    }
}
