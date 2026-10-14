package com.vplayed.core.ui.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vplayed.core.designsystem.theme.AppearanceConfig
import com.vplayed.core.repository.AppearanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface AppearanceUiState {
    /** Preferences not read yet; the system splash stays on screen to avoid a theme flash. */
    data object Loading : AppearanceUiState
    data class Ready(val appearance: AppearanceConfig) : AppearanceUiState
}

/**
 * Activity-scoped appearance for [AppTheme][com.vplayed.core.designsystem.theme.AppTheme]. Owned by
 * [BaseActivity]; [BaseFragment]s read the same instance, so Compose screens and Fragment-hosted
 * screens always share one theme.
 */
@HiltViewModel
class AppearanceViewModel @Inject constructor(repository: AppearanceRepository) : ViewModel() {
    val uiState: StateFlow<AppearanceUiState> = repository.appearance
        .map<AppearanceConfig, AppearanceUiState> { AppearanceUiState.Ready(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppearanceUiState.Loading)
}
