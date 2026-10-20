package com.vplayed.feature.auth.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vplayed.feature.auth.domain.DeviceLimitHandoff
import com.vplayed.shared.auth.AuthFlowConfig
import com.vplayed.shared.auth.AuthMessage
import com.vplayed.shared.auth.AuthMessageKey
import com.vplayed.shared.auth.AuthRepository
import com.vplayed.shared.auth.OtpChannelKind
import com.vplayed.shared.auth.OtpController
import com.vplayed.shared.auth.OtpEffect
import com.vplayed.shared.auth.OtpState
import com.vplayed.shared.auth.PendingRegistration
import com.vplayed.shared.auth.RegistrationController
import com.vplayed.shared.auth.RegistrationEffect
import com.vplayed.shared.auth.RegistrationState
import com.vplayed.shared.auth.SignedInDevice
import com.vplayed.shared.phone.CountryDialCode
import com.vplayed.shared.phone.PhoneNumber
import com.vplayed.shared.profile.SessionProfileChooser
import com.vplayed.shared.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** The OTP screen's state: the shared controller's state (`../kmp`, `auth/OtpController`). */
typealias OtpUiState = OtpState

sealed interface OtpEvent {
    /** Leave the OTP screen for the email step (Back, Change). */
    data object NavigateBack : OtpEvent

    /** Signed in and the success step has been shown: go to the Dashboard. */
    data object Authenticated : OtpEvent

    /** Sign-up was left unfinished: back to the email step, saying so in a Toast ("Your signup was unsuccessful."). */
    data class LeftSignUp(val message: AuthMessage) : OtpEvent
}

/**
 * Code step of email or phone login, the device-limit step and the success step (prototype "VPlayed
 * Sign Up" steps 2–3) on the shared [OtpController], which holds the rules: six digits, one request
 * at a time, the 60 s resend countdown, the device flows and the 3 s success step. Phone codes are
 * texted and checked by Firebase ([AuthChannels.sender]); Google sign-in at its device limit uses
 * only the device step. A new account then fills in the sign-up form on this screen
 * ([RegistrationController], [registrationState]) before it is signed in. Every sign-in uses the
 * account's first profile ([AuthChannels.profileChooser]; no profile picker yet). This ViewModel
 * stores the session the controllers hand over and turns their other effects into navigation.
 *
 * Arguments (from the fragment arguments through [SavedStateHandle]): [ARG_EMAIL] (the email, or
 * the national number), [ARG_START_WITH_DEVICE_LIMIT] (the login answer was already "device limit";
 * the device list comes from [DeviceLimitHandoff]), [ARG_CHANNEL] and [ARG_DIAL_CODE].
 */
@HiltViewModel
class OtpViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: AuthRepository,
    deviceLimitHandoff: DeviceLimitHandoff,
    private val sessions: SessionManager,
    /** Phone and Google sign-in; null in unit tests of email sign-in. */
    channels: AuthChannels? = null,
) : ViewModel() {

    private val email: String = savedStateHandle.get<String>(ARG_EMAIL).orEmpty()

    private val channel: OtpChannelKind =
        OtpChannelKind.entries.firstOrNull { it.name == savedStateHandle.get<String>(ARG_CHANNEL) } ?: OtpChannelKind.EMAIL

    private val initialDeviceLimit =
        if (savedStateHandle.get<Boolean>(ARG_START_WITH_DEVICE_LIMIT) == true) deviceLimitHandoff.take() else null

    /** Google sign-in's credential and device list; gone after process death (then the screen closes). */
    private val socialStart = if (channel == OtpChannelKind.SOCIAL) channels?.socialHandoff?.take() else null

    private val profileChooser = channels?.profileChooser ?: SessionProfileChooser.KeepDefault

    private val registrationRepository = channels?.registration

    private val controller: OtpController = when {
        channel == OtpChannelKind.PHONE && channels != null -> OtpController.phone(
            phone = PhoneNumber(savedStateHandle.get<String>(ARG_DIAL_CODE).orEmpty(), email),
            repository = channels.phone,
            accounts = repository,
            sender = channels.sender,
            initialDeviceLimit = initialDeviceLimit,
            scope = viewModelScope,
            config = AUTH_FLOW_CONFIG,
            profileChooser = profileChooser,
        )
        socialStart != null && channels != null -> OtpController.socialDevices(
            credential = socialStart.first,
            repository = channels.social,
            accounts = repository,
            limit = socialStart.second,
            scope = viewModelScope,
            config = AUTH_FLOW_CONFIG,
            profileChooser = profileChooser,
        )
        else -> OtpController(
            email = email,
            repository = repository,
            initialDeviceLimit = initialDeviceLimit,
            scope = viewModelScope,
            config = AUTH_FLOW_CONFIG,
            profileChooser = profileChooser,
        )
    }

    val uiState: StateFlow<OtpUiState> = controller.state

    private var registration: RegistrationController? = null
    private val _registrationState = MutableStateFlow<RegistrationState?>(null)

    /** The sign-up form, once the code step verified a new account; null until then. */
    val registrationState: StateFlow<RegistrationState?> = _registrationState.asStateFlow()

    private val _events = Channel<OtpEvent>(Channel.BUFFERED)
    val events: Flow<OtpEvent> = _events.receiveAsFlow()

    init {
        // Nothing to show without the Google sign-in it continues.
        if (channel == OtpChannelKind.SOCIAL && socialStart == null) viewModelScope.launch { _events.send(OtpEvent.NavigateBack) }
        // One collector, in order: the session is stored before Authenticated is passed on.
        viewModelScope.launch {
            controller.effects.collect { effect ->
                when (effect) {
                    is OtpEffect.SignedIn -> sessions.signIn(effect.session)
                    OtpEffect.Authenticated -> _events.send(OtpEvent.Authenticated)
                    OtpEffect.NavigateBack -> _events.send(OtpEvent.NavigateBack)
                    is OtpEffect.RegistrationNeeded -> startRegistration(effect.registration)
                }
            }
        }
    }

    private suspend fun startRegistration(pending: PendingRegistration) {
        val repository = registrationRepository
        if (repository == null || registration != null) {
            _events.send(OtpEvent.LeftSignUp(AuthMessage.Localized(AuthMessageKey.REGISTRATION_FAILED)))
            return
        }
        val form = RegistrationController(pending, repository, viewModelScope, AUTH_FLOW_CONFIG, profileChooser)
        registration = form
        viewModelScope.launch { form.state.collect { _registrationState.value = it } }
        // One collector, in order: the session is stored before Authenticated is passed on.
        viewModelScope.launch {
            form.effects.collect { effect ->
                when (effect) {
                    is RegistrationEffect.SignedIn -> sessions.signIn(effect.session)
                    RegistrationEffect.Authenticated -> _events.send(OtpEvent.Authenticated)
                    is RegistrationEffect.Cancelled -> _events.send(OtpEvent.LeftSignUp(effect.message))
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

    // Sign-up form

    fun onNameChange(value: String) {
        registration?.onNameChange(value)
    }

    fun onRegistrationEmailChange(value: String) {
        registration?.onEmailChange(value)
    }

    fun onRegistrationCountryChange(country: CountryDialCode) {
        registration?.onCountryChange(country)
    }

    fun onRegistrationNumberChange(value: String) {
        registration?.onNumberChange(value)
    }

    fun onShowReferral() {
        registration?.onShowReferral()
    }

    fun onReferralChange(value: String) {
        registration?.onReferralChange(value)
    }

    fun onSubmitRegistration() {
        registration?.onSubmit()
    }

    /** Back on the sign-up form: leaves it (nothing was stored). */
    fun onRegistrationBack() {
        registration?.onBack()
    }

    companion object {
        const val ARG_EMAIL = "email"
        const val ARG_START_WITH_DEVICE_LIMIT = "start_with_device_limit"
        const val ARG_CHANNEL = "channel"
        const val ARG_DIAL_CODE = "dial_code"

        /** Digits in a login code (prototype and previous app). */
        const val CODE_LENGTH = AuthFlowConfig.CODE_LENGTH

        /** Seconds before a new code can be requested (previous app; the prototype uses 30). */
        const val RESEND_SECONDS = AuthFlowConfig.RESEND_SECONDS

        /** How long the success step stays before the Dashboard opens (prototype: 3 s). */
        const val SUCCESS_MILLIS = AuthFlowConfig.SUCCESS_MILLIS
    }
}
