package com.vplayed.feature.auth.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vplayed.core.session.SessionRepository
import com.vplayed.feature.auth.domain.DeviceLimitHandoff
import com.vplayed.shared.auth.AuthMessage
import com.vplayed.shared.auth.AuthRepository
import com.vplayed.shared.auth.EmailLoginController
import com.vplayed.shared.auth.EmailLoginEffect
import com.vplayed.shared.auth.EmailLoginState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** The email step's screen state: the shared controller's state (`../kmp`, `auth/EmailLoginController`). */
typealias LoginUiState = EmailLoginState

sealed interface LoginEvent {
    /** Shown as a Toast. */
    data class ShowMessage(val message: AuthMessage) : LoginEvent

    /** The backend sent a code (or the account is at its device limit): open the OTP screen. */
    data class OpenOtp(val email: String, val startWithDeviceLimit: Boolean) : LoginEvent
}

/**
 * Email step of login on the shared [EmailLoginController], which validates the address, requests a
 * code once at a time and reads the answer. This ViewModel turns its effects into navigation and
 * Toasts: the OTP screen opens when a code was sent or the account is at its device limit with a
 * device list to act on (handed over through [DeviceLimitHandoff]).
 *
 * It is scoped to the activity so the typed email survives the trip to the OTP screen and back
 * (the Login fragment is removed while OTP is shown); it is cleared on sign-in and when Login is closed.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    repository: AuthRepository,
    private val deviceLimitHandoff: DeviceLimitHandoff,
    sessions: SessionRepository,
) : ViewModel() {

    private val controller = EmailLoginController(repository, viewModelScope, AUTH_FLOW_CONFIG)

    val uiState: StateFlow<LoginUiState> = controller.state

    private val _events = Channel<LoginEvent>(Channel.BUFFERED)
    val events: Flow<LoginEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch { controller.effects.collect { effect -> effect.toEvent()?.let { _events.send(it) } } }
        // Once signed in, the typed email has no further use.
        viewModelScope.launch { sessions.session.filterNotNull().collect { controller.clear() } }
    }

    fun onEmailChange(value: String) = controller.onEmailChange(value)

    /** Like the prototype: a non-empty, malformed address is flagged when the field loses focus. */
    fun onEmailFocusLost() = controller.onEmailFocusLost()

    /** Login is being closed (close button, guest): forget the typed email. */
    fun onClosed() = controller.onClosed()

    fun onSubmit() = controller.onSubmit()

    private fun EmailLoginEffect.toEvent(): LoginEvent? = when (this) {
        is EmailLoginEffect.CodeSent -> LoginEvent.OpenOtp(email, startWithDeviceLimit = false)
        is EmailLoginEffect.ShowDevices -> {
            deviceLimitHandoff.put(limit)
            LoginEvent.OpenOtp(email, startWithDeviceLimit = true)
        }
        is EmailLoginEffect.ShowMessage -> LoginEvent.ShowMessage(message)
        // This app has no guest entry point.
        is EmailLoginEffect.GuestSignedIn -> null
    }
}
