package com.vplayed.feature.splashscreen.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vplayed.core.repository.AppearanceRepository
import com.vplayed.feature.splashscreen.di.SplashConfig
import com.vplayed.shared.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface SplashUiState {
    data object Starting : SplashUiState

    /** Startup work is done; the app may leave the splash screen. */
    data object Ready : SplashUiState
}

/**
 * Runs app-start work while the splash screen is visible. The splash stays for at least
 * [SplashConfig.minDisplayMillis] (3 s); startup work runs in parallel, so a slow start does
 * not add to that time.
 *
 * Add future startup steps — remote config, choosing the first destination — to [runStartup].
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val appearanceRepository: AppearanceRepository,
    private val sessionManager: SessionManager,
    private val config: SplashConfig,
) : ViewModel() {

    private val _uiState = MutableStateFlow<SplashUiState>(SplashUiState.Starting)
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val minimumDisplay = async { delay(config.minDisplayMillis) }
            runStartup()
            minimumDisplay.await()
            _uiState.value = SplashUiState.Ready
        }
    }

    private suspend fun runStartup() {
        // Appearance must be known before the first themed screen is shown.
        appearanceRepository.appearance.first()
        // Read the stored session now, so the menu shows the right account when the app opens
        // (backend requests would also wait for it).
        sessionManager.awaitLoaded()
    }
}
