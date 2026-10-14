package com.vplayed.reference.showcase.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.vplayed.core.common.config.AppConfig
import com.vplayed.core.common.validation.EmailValidationResult
import com.vplayed.core.common.validation.EmailValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Which content state the demo panel shows. */
enum class DemoContentState { CONTENT, LOADING, EMPTY, ERROR }

data class EnvironmentInfo(
    val environmentId: String,
    val serverUrl: String,
    val versionName: String,
)

data class DesignShowcaseUiState(
    val environment: EnvironmentInfo,
    val email: String = "",
    /** Set only after a submit attempt, so the field does not show an error while typing the first time. */
    val emailError: EmailValidationResult? = null,
    val emailAccepted: Boolean = false,
    val demoState: DemoContentState = DemoContentState.CONTENT,
)

@HiltViewModel
class DesignShowcaseViewModel @Inject constructor(config: AppConfig) : ViewModel() {

    private val _uiState = MutableStateFlow(
        DesignShowcaseUiState(
            environment = EnvironmentInfo(
                environmentId = config.environment.id,
                serverUrl = config.serverUrl,
                versionName = config.versionName,
            ),
        ),
    )
    val uiState: StateFlow<DesignShowcaseUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, emailError = null, emailAccepted = false) }
    }

    fun onSubmitEmail() {
        _uiState.update { state ->
            val result = EmailValidator.validate(state.email)
            state.copy(
                emailError = result.takeUnless { it == EmailValidationResult.VALID },
                emailAccepted = result == EmailValidationResult.VALID,
            )
        }
    }

    fun onDemoStateSelected(state: DemoContentState) {
        _uiState.update { it.copy(demoState = state) }
    }
}
