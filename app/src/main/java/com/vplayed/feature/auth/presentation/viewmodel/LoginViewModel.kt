package com.vplayed.feature.auth.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vplayed.R
import com.vplayed.core.common.validation.EmailValidationResult
import com.vplayed.core.common.validation.EmailValidator
import com.vplayed.core.session.SessionRepository
import com.vplayed.feature.auth.domain.AuthRepository
import com.vplayed.feature.auth.domain.DeviceLimitHandoff
import com.vplayed.feature.auth.domain.EmailLoginResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    /** EMPTY or INVALID_FORMAT while the field shows an error; null otherwise. */
    val emailError: EmailValidationResult? = null,
    /** A login request is in flight: the button shows progress and input is locked. */
    val isSubmitting: Boolean = false,
)

sealed interface LoginEvent {
    /** Shown as a Toast. */
    data class ShowMessage(val message: AuthMessage) : LoginEvent

    /** The backend sent a code (or the account is at its device limit): open the OTP screen. */
    data class OpenOtp(val email: String, val startWithDeviceLimit: Boolean) : LoginEvent
}

/**
 * Email step of login: validates the address and requests a login code once at a time. The OTP
 * screen opens only when the backend says a code was sent (HTTP 200/203 with no API error) or the
 * account is at its device limit with a device list to act on; everything else is a Toast.
 *
 * It is scoped to the activity so the typed email survives the trip to the OTP screen and back
 * (the Login fragment is removed while OTP is shown); it is cleared on sign-in and when Login is closed.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val deviceLimitHandoff: DeviceLimitHandoff,
    sessions: SessionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _events = Channel<LoginEvent>(Channel.BUFFERED)
    val events: Flow<LoginEvent> = _events.receiveAsFlow()

    init {
        // Once signed in, the typed email has no further use.
        viewModelScope.launch {
            sessions.session.filterNotNull().collect { _uiState.value = LoginUiState() }
        }
    }

    fun onEmailChange(value: String) {
        _uiState.update { if (it.isSubmitting) it else it.copy(email = value, emailError = null) }
    }

    /** Like the prototype: a non-empty, malformed address is flagged when the field loses focus. */
    fun onEmailFocusLost() {
        val state = _uiState.value
        if (state.isSubmitting || state.email.isBlank()) return
        val result = EmailValidator.validate(state.email)
        if (result != EmailValidationResult.VALID) _uiState.update { it.copy(emailError = result) }
    }

    /** Login is being closed (close button, guest): forget the typed email. */
    fun onClosed() {
        if (!_uiState.value.isSubmitting) _uiState.value = LoginUiState()
    }

    fun onSubmit() {
        val state = _uiState.value
        if (state.isSubmitting) return
        val validation = EmailValidator.validate(state.email)
        if (validation != EmailValidationResult.VALID) {
            _uiState.update { it.copy(emailError = validation) }
            return
        }
        _uiState.update { it.copy(isSubmitting = true, emailError = null) }
        viewModelScope.launch {
            try {
                _events.send(repository.requestEmailLogin(state.email).toEvent(state.email.trim()))
            } finally {
                _uiState.update { it.copy(isSubmitting = false) }
            }
        }
    }

    private fun EmailLoginResult.toEvent(email: String): LoginEvent = when (this) {
        is EmailLoginResult.CodeSent -> LoginEvent.OpenOtp(email, startWithDeviceLimit = false)
        is EmailLoginResult.DeviceLimitReached ->
            if (limit != null) {
                deviceLimitHandoff.put(limit)
                LoginEvent.OpenOtp(email, startWithDeviceLimit = true)
            } else {
                LoginEvent.ShowMessage(AuthMessage.serverOr(message, R.string.login_device_limit))
            }
        is EmailLoginResult.Rejected -> LoginEvent.ShowMessage(AuthMessage.serverOr(message, R.string.login_failed))
        is EmailLoginResult.Failure -> LoginEvent.ShowMessage(AuthMessage.from(error))
    }
}
