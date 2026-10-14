package com.vplayed.feature.settings.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vplayed.core.designsystem.dimension.UiDensity
import com.vplayed.core.designsystem.theme.AppearanceConfig
import com.vplayed.core.designsystem.theme.ThemeMode
import com.vplayed.core.repository.AppearanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AppearanceSettingsUiState {
    data object Loading : AppearanceSettingsUiState
    data class Ready(val appearance: AppearanceConfig) : AppearanceSettingsUiState
}

/** Writes go straight to [AppearanceRepository]; the UI re-renders from the repository's flow. */
@HiltViewModel
class AppearanceSettingsViewModel @Inject constructor(
    private val repository: AppearanceRepository,
) : ViewModel() {

    val uiState: StateFlow<AppearanceSettingsUiState> = repository.appearance
        .map<AppearanceConfig, AppearanceSettingsUiState> { AppearanceSettingsUiState.Ready(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = AppearanceSettingsUiState.Loading,
        )

    fun onThemeModeSelected(mode: ThemeMode) = update { setThemeMode(mode) }
    fun onPaletteSelected(paletteId: String) = update { setPalette(paletteId) }
    fun onFontSelected(fontId: String) = update { setFont(fontId) }
    fun onTextScaleChanged(scale: Float) = update { setTextScale(scale) }
    fun onDensitySelected(density: UiDensity) = update { setDensity(density) }
    fun onDynamicColorChanged(enabled: Boolean) = update { setDynamicColor(enabled) }
    fun onResetClicked() = update { reset() }

    private fun update(block: suspend AppearanceRepository.() -> Unit) {
        viewModelScope.launch { repository.block() }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
