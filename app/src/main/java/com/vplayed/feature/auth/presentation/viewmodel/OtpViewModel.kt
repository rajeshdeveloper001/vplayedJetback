package com.vplayed.feature.auth.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vplayed.core.session.SessionRepository
import com.vplayed.feature.auth.data.toUserSession
import com.vplayed.feature.auth.domain.DeviceLimitHandoff
import com.vplayed.shared.auth.AuthFlowConfig
import com.vplayed.shared.auth.AuthRepository
import com.vplayed.shared.auth.OtpController
import com.vplayed.shared.auth.OtpEffect
import com.vplayed.shared.auth.OtpState
import com.vplayed.shared.auth.SignedInDevice
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** The OTP screen's state: the shared controller's state (`../kmp`, `auth/OtpController`). */
typealias OtpUiState = OtpState

sealed interface OtpEvent {
    /** Leave the OTP screen for the email step (Back, Change). */
    data object NavigateBack : OtpEvent

    /** Signed in and the success step has been shown: go to the Dashboard. */
    data object Authenticated : OtpEvent
}

/**
 * Code step of email login, the device-limit step and the success step (prototype "VPlayed Sign Up"
 * steps 2–3) on the shared [OtpController], which holds the rules: six digits, one request at a
 * time, the 60 s resend countdown, the device flows and the 3 s success step. This ViewModel stores
 * the session the controller hands over and turns its other effects into navigation.
 *
 * Arguments (from the fragment arguments through [SavedStateHandle]): [ARG_EMAIL] and
 * [ARG_START_WITH_DEVICE_LIMIT] (the email-login answer was already "device limit"; the device
 * list comes from [DeviceLimitHandoff]).
 */
@HiltViewModel
class OtpViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: AuthRepository,
    deviceLimitHandoff: DeviceLimitHandoff,
    private val sessions: SessionRepository,
) : ViewModel() {

    private val email: String = savedStateHandle.get<String>(ARG_EMAIL).orEmpty()

    private val controller = OtpController(
        email = email,
        repository = repository,
        initialDeviceLimit = if (savedStateHandle.get<Boolean>(ARG_START_WITH_DEVICE_LIMIT) == true) deviceLimitHandoff.take() else null,
        scope = viewModelScope,
        config = AUTH_FLOW_CONFIG,
    )

    val uiState: StateFlow<OtpUiState> = controller.state

    private val _events = Channel<OtpEvent>(Channel.BUFFERED)
    val events: Flow<OtpEvent> = _events.receiveAsFlow()

    init {
        // One collector, in order: the session is stored before Authenticated is passed on.
        viewModelScope.launch {
            controller.effects.collect { effect ->
                when (effect) {
                    is OtpEffect.SignedIn -> sessions.save(effect.session.toUserSession(email.trim()))
                    OtpEffect.Authenticated -> _events.send(OtpEvent.Authenticated)
                    OtpEffect.NavigateBack -> _events.send(OtpEvent.NavigateBack)
                }
            }
        }
    }

    fun onCodeChange(value: String) = controller.onCodeChange(value)

    fun onVerify() = controller.onVerify()

    fun onResend() = controller.onResend()

    /** Back arrow, "Change" and the system Back button. */
    fun onBack() = controller.onBack()

    fun onSignOutDevice(device: SignedInDevice) = controller.onSignOutDevice(device)

    fun onSignOutAllDevices() = controller.onSignOutAllDevices()

    fun onContinueAfterSignOut() = controller.onContinueAfterSignOut()

    companion object {
        const val ARG_EMAIL = "email"
        const val ARG_START_WITH_DEVICE_LIMIT = "start_with_device_limit"

        /** Digits in a login code (prototype and previous app). */
        const val CODE_LENGTH = AuthFlowConfig.CODE_LENGTH

        /** Seconds before a new code can be requested (previous app; the prototype uses 30). */
        const val RESEND_SECONDS = AuthFlowConfig.RESEND_SECONDS

        /** How long the success step stays before the Dashboard opens (prototype: 3 s). */
        const val SUCCESS_MILLIS = AuthFlowConfig.SUCCESS_MILLIS
    }
}
