package com.vplayed.feature.auth.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vplayed.R
import com.vplayed.feature.auth.domain.AuthRepository
import com.vplayed.feature.auth.domain.DeviceLimitHandoff
import com.vplayed.feature.auth.domain.EmailLoginResult
import com.vplayed.feature.auth.domain.model.DeviceLimit
import com.vplayed.feature.auth.domain.model.OtpVerificationResult
import com.vplayed.feature.auth.domain.model.SignOutDeviceResult
import com.vplayed.feature.auth.domain.model.SignedInDevice
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class OtpStep { Code, Devices, Success }

data class OtpUiState(
    val email: String,
    val step: OtpStep = OtpStep.Code,
    /** Digits typed so far (0–[OtpViewModel.CODE_LENGTH]). */
    val code: String = "",
    /**
     * Inline error under the code boxes (or the device list): a refused code, a failed resend or
     * sign-out, or no connection. Cleared by the next edit or action. No Toasts on this screen.
     */
    val error: AuthMessage? = null,
    /** Bumped for each refused code; the screen shakes once per change (prototype `wpShake`). */
    val shakeCount: Int = 0,
    /** Informational line on the success step (profile setup / selection are not built yet). */
    val notice: AuthMessage? = null,
    val isVerifying: Boolean = false,
    /** Seconds until a new code can be requested; 0 = the Resend action is available. */
    val resendSecondsLeft: Int = 0,
    val isResending: Boolean = false,
    val devices: List<SignedInDevice> = emptyList(),
    val maxDevices: Int? = null,
    /** A sign-out or continue request is in flight on the device step. */
    val isSigningOut: Boolean = false,
    /** At least one device was signed out: the device step offers Continue. */
    val hasSignedOutDevice: Boolean = false,
) {
    val canVerify: Boolean get() = code.length == OtpViewModel.CODE_LENGTH && !isVerifying
    val canResend: Boolean get() = resendSecondsLeft == 0 && !isResending && !isVerifying
}

sealed interface OtpEvent {
    /** Leave the OTP screen for the email step (Back, Change). */
    data object NavigateBack : OtpEvent

    /** Signed in and the success step has been shown: go to the Dashboard. */
    data object Authenticated : OtpEvent
}

/**
 * Code step of email login, the device-limit step and the success step (prototype "VPlayed Sign Up"
 * steps 2–3). Every answer is interpreted from the repository's typed results, never from "HTTP
 * 200"; a code is verified only when it has exactly [CODE_LENGTH] digits and nothing else is in
 * flight. The countdown runs in [viewModelScope], so it ends with the screen.
 *
 * Arguments (from the fragment arguments through [SavedStateHandle]): [ARG_EMAIL] and
 * [ARG_START_WITH_DEVICE_LIMIT] (the email-login answer was already "device limit").
 */
@HiltViewModel
class OtpViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: AuthRepository,
    deviceLimitHandoff: DeviceLimitHandoff,
) : ViewModel() {

    private val email: String = savedStateHandle.get<String>(ARG_EMAIL).orEmpty()

    /** Where a device-limit answer came from decides what "Continue" and Back do. */
    private enum class DeviceFlow { AfterEmailLogin, AfterVerify }

    private var deviceFlow = DeviceFlow.AfterVerify
    private var countdownJob: Job? = null

    private val _uiState = MutableStateFlow(OtpUiState(email = email))
    val uiState: StateFlow<OtpUiState> = _uiState.asStateFlow()

    private val _events = Channel<OtpEvent>(Channel.BUFFERED)
    val events: Flow<OtpEvent> = _events.receiveAsFlow()

    init {
        val limit = if (savedStateHandle.get<Boolean>(ARG_START_WITH_DEVICE_LIMIT) == true) deviceLimitHandoff.take() else null
        if (limit != null) {
            showDevices(limit, DeviceFlow.AfterEmailLogin)
        } else {
            // Login just asked the backend to send the code.
            startCountdown()
        }
    }

    fun onCodeChange(value: String) {
        _uiState.update { state ->
            if (state.isVerifying || state.step != OtpStep.Code) {
                state
            } else {
                state.copy(code = value.filter(Char::isDigit).take(CODE_LENGTH), error = null)
            }
        }
    }

    fun onVerify() {
        val state = _uiState.value
        if (state.step != OtpStep.Code || !state.canVerify) return
        _uiState.update { it.copy(isVerifying = true, error = null) }
        viewModelScope.launch {
            try {
                handleVerification(repository.verifyEmailOtp(email, state.code), fromCodeStep = true)
            } finally {
                _uiState.update { it.copy(isVerifying = false) }
            }
        }
    }

    fun onResend() {
        if (_uiState.value.step != OtpStep.Code || !_uiState.value.canResend) return
        _uiState.update { it.copy(isResending = true, error = null) }
        viewModelScope.launch {
            try {
                when (val result = repository.requestEmailLogin(email)) {
                    is EmailLoginResult.CodeSent -> {
                        // A new code replaces the old one, so the boxes start empty again. The restarted
                        // countdown is the feedback; there is no message.
                        _uiState.update { it.copy(code = "", error = null) }
                        startCountdown()
                    }
                    is EmailLoginResult.DeviceLimitReached ->
                        result.limit?.let { showDevices(it, DeviceFlow.AfterEmailLogin) }
                            ?: showError(AuthMessage.serverOr(result.message, R.string.login_device_limit))
                    is EmailLoginResult.Rejected -> showError(AuthMessage.serverOr(result.message, R.string.login_failed))
                    is EmailLoginResult.Failure -> showError(AuthMessage.from(result.error))
                }
            } finally {
                _uiState.update { it.copy(isResending = false) }
            }
        }
    }

    /** Back arrow, "Change" and the system Back button. */
    fun onBack() {
        val state = _uiState.value
        when {
            // Busy or celebrating: stay put.
            state.isVerifying || state.isSigningOut || state.step == OtpStep.Success -> Unit
            state.step == OtpStep.Devices && deviceFlow == DeviceFlow.AfterVerify ->
                _uiState.update { it.copy(step = OtpStep.Code, code = "", error = null, devices = emptyList()) }
            else -> viewModelScope.launch { _events.send(OtpEvent.NavigateBack) }
        }
    }

    // region devices

    fun onSignOutDevice(device: SignedInDevice) {
        if (_uiState.value.isSigningOut || device !in _uiState.value.devices) return
        signOut(device, all = false)
    }

    fun onSignOutAllDevices() {
        val first = _uiState.value.devices.firstOrNull() ?: return
        if (_uiState.value.isSigningOut) return
        signOut(first, all = true)
    }

    fun onContinueAfterSignOut() {
        val state = _uiState.value
        if (state.isSigningOut || state.step != OtpStep.Devices || !state.hasSignedOutDevice) return
        continueAfterSignOut()
    }

    private fun signOut(device: SignedInDevice, all: Boolean) {
        _uiState.update { it.copy(isSigningOut = true, error = null) }
        viewModelScope.launch {
            var continueNow = false
            try {
                when (val result = repository.signOutDevice(device.signOutKey, all)) {
                    SignOutDeviceResult.Success -> {
                        val remaining = if (all) emptyList() else _uiState.value.devices - device
                        // The row leaving the list is the feedback. Prototype: with no devices left the flow
                        // continues by itself.
                        _uiState.update { it.copy(devices = remaining, hasSignedOutDevice = true) }
                        continueNow = remaining.isEmpty()
                    }
                    is SignOutDeviceResult.Rejected -> showError(AuthMessage.serverOr(result.message, R.string.otp_device_sign_out_failed))
                    is SignOutDeviceResult.Failure -> showError(AuthMessage.from(result.error))
                }
            } finally {
                _uiState.update { it.copy(isSigningOut = false) }
            }
            if (continueNow) continueAfterSignOut()
        }
    }

    private fun continueAfterSignOut() {
        _uiState.update { it.copy(isSigningOut = true, error = null) }
        viewModelScope.launch {
            try {
                when (deviceFlow) {
                    DeviceFlow.AfterVerify -> handleVerification(repository.continueAfterDeviceSignOut(email), fromCodeStep = false)
                    DeviceFlow.AfterEmailLogin -> when (val result = repository.requestEmailLogin(email)) {
                        is EmailLoginResult.CodeSent -> {
                            _uiState.update { it.copy(step = OtpStep.Code, code = "", error = null, devices = emptyList()) }
                            startCountdown()
                        }
                        is EmailLoginResult.DeviceLimitReached ->
                            result.limit?.let { showDevices(it, DeviceFlow.AfterEmailLogin) }
                                ?: showError(AuthMessage.serverOr(result.message, R.string.login_device_limit))
                        is EmailLoginResult.Rejected -> showError(AuthMessage.serverOr(result.message, R.string.login_failed))
                        is EmailLoginResult.Failure -> showError(AuthMessage.from(result.error))
                    }
                }
            } finally {
                _uiState.update { it.copy(isSigningOut = false) }
            }
        }
    }

    private fun showDevices(limit: DeviceLimit, flow: DeviceFlow) {
        deviceFlow = flow
        countdownJob?.cancel()
        _uiState.update {
            it.copy(
                step = OtpStep.Devices,
                error = null,
                devices = limit.devices,
                maxDevices = limit.maxDevices,
                hasSignedOutDevice = false,
                resendSecondsLeft = 0,
            )
        }
    }

    // endregion

    private suspend fun handleVerification(result: OtpVerificationResult, fromCodeStep: Boolean) {
        when (result) {
            is OtpVerificationResult.Verified -> {
                countdownJob?.cancel()
                // Registration and profile choice screens do not exist yet: say so on the success step
                // and use the default profile.
                val notice = when {
                    result.isNewUser -> AuthMessage.Resource(R.string.otp_new_user_pending)
                    result.hasMultipleProfiles -> AuthMessage.Resource(R.string.otp_multi_profile_pending)
                    else -> null
                }
                _uiState.update { it.copy(step = OtpStep.Success, code = "", error = null, notice = notice, resendSecondsLeft = 0) }
                viewModelScope.launch {
                    delay(SUCCESS_MILLIS)
                    _events.send(OtpEvent.Authenticated)
                }
            }
            is OtpVerificationResult.RegistrationRequired ->
                showError(AuthMessage.serverOr(result.message, R.string.otp_registration_pending))
            is OtpVerificationResult.DeviceLimitReached ->
                result.limit?.let { showDevices(it, DeviceFlow.AfterVerify) }
                    ?: showError(AuthMessage.serverOr(result.message, R.string.login_device_limit))
            // Invalid and expired codes are both "refused"; the server's message says which. As the
            // prototype's incorrect-PIN card: inline message, one shake, boxes cleared.
            is OtpVerificationResult.Rejected ->
                showError(AuthMessage.serverOr(result.message, R.string.otp_invalid), shake = fromCodeStep, clearCode = fromCodeStep)
            is OtpVerificationResult.Failure -> showError(AuthMessage.from(result.error))
        }
    }

    private fun startCountdown(seconds: Int = RESEND_SECONDS) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            for (left in seconds downTo 1) {
                _uiState.update { it.copy(resendSecondsLeft = left) }
                delay(1_000)
            }
            _uiState.update { it.copy(resendSecondsLeft = 0) }
        }
    }

    /** Shows [message] inline. A refused code also shakes the card once and empties the boxes (prototype). */
    private fun showError(message: AuthMessage, shake: Boolean = false, clearCode: Boolean = false) {
        _uiState.update {
            it.copy(
                error = message,
                shakeCount = if (shake) it.shakeCount + 1 else it.shakeCount,
                code = if (clearCode) "" else it.code,
            )
        }
    }

    companion object {
        const val ARG_EMAIL = "email"
        const val ARG_START_WITH_DEVICE_LIMIT = "start_with_device_limit"

        /** Digits in a login code (prototype and previous app). */
        const val CODE_LENGTH = 6

        /** Seconds before a new code can be requested (previous app; the prototype uses 30). */
        const val RESEND_SECONDS = 60

        /** How long the success step stays before the Dashboard opens (prototype: 3 s). */
        const val SUCCESS_MILLIS = 3_000L
    }
}
