package com.vplayed.feature.auth.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vplayed.feature.auth.data.GoogleSignInOutcome
import com.vplayed.feature.auth.domain.DeviceLimitHandoff
import com.vplayed.shared.auth.AuthMessage
import com.vplayed.shared.auth.AuthRepository
import com.vplayed.shared.auth.EmailLoginController
import com.vplayed.shared.auth.EmailLoginEffect
import com.vplayed.shared.auth.EmailLoginState
import com.vplayed.shared.auth.OtpChannelKind
import com.vplayed.shared.auth.PhoneLoginController
import com.vplayed.shared.auth.PhoneLoginEffect
import com.vplayed.shared.auth.PhoneLoginState
import com.vplayed.shared.auth.SocialLoginController
import com.vplayed.shared.auth.SocialLoginEffect
import com.vplayed.shared.auth.SocialProvider
import com.vplayed.shared.phone.CountryDialCode
import com.vplayed.shared.phone.CountryDialCodes
import com.vplayed.shared.session.SessionManager
import com.vplayed.shared.session.SessionStatus
import com.vplayed.shared.settings.VerificationMethod
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The email step's screen state: the shared controller's state (`../kmp`, `auth/EmailLoginController`). */
typealias LoginUiState = EmailLoginState

/** What else step 1 offers, beyond the email form. */
data class LoginOptions(
    /** Ask for a mobile number instead of an email (common settings `otp_verfied`, Firebase configured). */
    val usesPhone: Boolean = false,
    val phone: PhoneLoginState = PhoneLoginState(country = CountryDialCodes.DEFAULT),
    val googleAvailable: Boolean = false,
    /** Common settings allow guest sessions (`is_guest_user`). */
    val guestAllowed: Boolean = false,
    /** The provider whose sheet or `auth/login` call is running. */
    val socialInFlight: SocialProvider? = null,
)

sealed interface LoginEvent {
    /** Shown as a Toast. */
    data class ShowMessage(val message: AuthMessage) : LoginEvent

    /**
     * Open the OTP screen: a code was requested, or the account is at its device limit. [destination]
     * is the email, or the national number with [dialCode] for phone sign-in; Google sign-in at its
     * device limit opens only the device step ([OtpChannelKind.SOCIAL]).
     */
    data class OpenOtp(
        val email: String,
        val startWithDeviceLimit: Boolean,
        val channel: OtpChannelKind = OtpChannelKind.EMAIL,
        val dialCode: String? = null,
    ) : LoginEvent

    /** A session started (guest, or Google with [showSignedIn]): leave Login. */
    data class Close(val showSignedIn: Boolean = false) : LoginEvent
}

/**
 * Step 1 of login on the shared controllers: [EmailLoginController] (or [PhoneLoginController] when
 * the common settings ask for phone verification), [SocialLoginController] for Google, and guest
 * access. This ViewModel turns their effects into navigation and Toasts: the OTP screen opens when
 * a code was requested or the account is at its device limit with a device list to act on (handed
 * over through [DeviceLimitHandoff] / `SocialLoginHandoff`).
 *
 * It is scoped to the activity so the typed email survives the trip to the OTP screen and back
 * (the Login fragment is removed while OTP is shown); it is cleared on sign-in and when Login is closed.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    repository: AuthRepository,
    private val deviceLimitHandoff: DeviceLimitHandoff,
    private val sessions: SessionManager,
    /** Phone and Google sign-in; null in unit tests of the email step. */
    private val channels: AuthChannels? = null,
) : ViewModel() {

    private val controller = EmailLoginController(repository, viewModelScope, AUTH_FLOW_CONFIG)

    private val phoneController = channels?.let {
        PhoneLoginController(it.phone, viewModelScope, AUTH_FLOW_CONFIG, CountryDialCodes.forRegion(Locale.getDefault().country) ?: CountryDialCodes.DEFAULT)
    }

    private val socialController = channels?.let { SocialLoginController(it.social, viewModelScope, AUTH_FLOW_CONFIG, it.profileChooser) }

    val uiState: StateFlow<LoginUiState> = controller.state

    val loginOptions: StateFlow<LoginOptions> = if (channels == null || phoneController == null || socialController == null) {
        MutableStateFlow(LoginOptions())
    } else {
        combine(
            channels.settings.settings,
            phoneController.state,
            socialController.state,
        ) { settings, phone, social ->
            LoginOptions(
                usesPhone = settings?.verificationMethod.let { it == VerificationMethod.PHONE || it == VerificationMethod.MOBILE } &&
                    channels.config.firebase != null,
                phone = phone,
                googleAvailable = channels.config.googleWebClientId.isNotBlank(),
                guestAllowed = settings?.isGuestUser == true,
                socialInFlight = social.inFlight,
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, LoginOptions())
    }

    private val _events = Channel<LoginEvent>(Channel.BUFFERED)
    val events: Flow<LoginEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch { controller.effects.collect { effect -> effect.toEvent()?.let { _events.send(it) } } }
        viewModelScope.launch { (phoneController?.effects ?: flowOf()).collect { _events.send(it.toEvent()) } }
        viewModelScope.launch { (socialController?.effects ?: flowOf()).collect { effect -> effect.toEvent()?.let { _events.send(it) } } }
        // Once signed in or signed out, clean up any previous input or controller state.
        viewModelScope.launch {
            sessions.status.collect { status ->
                if (status is SessionStatus.SignedIn || status is SessionStatus.SignedOut) {
                    controller.clear()
                    phoneController?.clear()
                    socialController?.clear()
                }
            }
        }
    }

    fun onEmailChange(value: String) = controller.onEmailChange(value)

    /** Like the prototype: a non-empty, malformed address is flagged when the field loses focus. */
    fun onEmailFocusLost() = controller.onEmailFocusLost()

    fun onPhoneChange(value: String) {
        phoneController?.onNumberChange(value)
    }

    fun onPhoneFocusLost() {
        phoneController?.onNumberFocusLost()
    }

    fun onCountryChange(country: CountryDialCode) {
        phoneController?.onCountryChange(country)
    }

    /** Login is being closed (close button): forget the typed email or number. */
    fun onClosed() {
        controller.onClosed()
        phoneController?.onClosed()
        socialController?.clear()
    }

    fun onSubmit() {
        if (loginOptions.value.usesPhone) phoneController?.onSubmit() else controller.onSubmit()
    }

    /** "Continue as guest": a guest session (`auth/generate`), unless the automatic one is already running. */
    fun onGuest() {
        if (sessions.session?.isGuest == true) {
            viewModelScope.launch { _events.send(LoginEvent.Close()) }
        } else {
            controller.joinAsGuest()
        }
    }

    /** The Google sheet is about to open; false when another sign-in is running. */
    fun onGoogleStarted(): Boolean {
        val social = socialController ?: return false
        if (social.state.value.inFlight != null || uiState.value.isSubmitting || uiState.value.isJoiningAsGuest) {
            return false
        }
        if (!social.canStart) {
            social.clear()
        }
        social.onProviderStarted(SocialProvider.GOOGLE)
        return true
    }

    fun onGoogleResult(outcome: GoogleSignInOutcome) {
        val social = socialController ?: return
        when (outcome) {
            is GoogleSignInOutcome.Credential -> social.onCredential(outcome.credential)
            GoogleSignInOutcome.Cancelled -> social.onProviderFinished(SocialProvider.GOOGLE, cancelled = true)
            GoogleSignInOutcome.Failed -> social.onProviderFinished(SocialProvider.GOOGLE, cancelled = false)
        }
    }

    private suspend fun EmailLoginEffect.toEvent(): LoginEvent? = when (this) {
        is EmailLoginEffect.CodeSent -> LoginEvent.OpenOtp(email, startWithDeviceLimit = false)
        is EmailLoginEffect.ShowDevices -> {
            deviceLimitHandoff.put(limit)
            LoginEvent.OpenOtp(email, startWithDeviceLimit = true)
        }
        is EmailLoginEffect.ShowMessage -> LoginEvent.ShowMessage(message)
        is EmailLoginEffect.GuestSignedIn -> {
            sessions.signIn(session)
            LoginEvent.Close()
        }
    }

    private fun PhoneLoginEffect.toEvent(): LoginEvent = when (this) {
        is PhoneLoginEffect.CodeRequested ->
            LoginEvent.OpenOtp(phone.nationalNumber, startWithDeviceLimit = false, OtpChannelKind.PHONE, phone.dialCode)
        is PhoneLoginEffect.ShowDevices -> {
            deviceLimitHandoff.put(limit)
            LoginEvent.OpenOtp(phone.nationalNumber, startWithDeviceLimit = true, OtpChannelKind.PHONE, phone.dialCode)
        }
        is PhoneLoginEffect.ShowMessage -> LoginEvent.ShowMessage(message)
    }

    private suspend fun SocialLoginEffect.toEvent(): LoginEvent? = when (this) {
        // As the previous app: store the session, say so, leave Login.
        is SocialLoginEffect.SignedIn -> {
            sessions.signIn(session)
            LoginEvent.Close(showSignedIn = true)
        }
        // The previous effect already closed Login.
        SocialLoginEffect.Authenticated -> null
        is SocialLoginEffect.ShowDevices -> {
            channels?.socialHandoff?.put(credential, limit)
            LoginEvent.OpenOtp(credential.email.orEmpty(), startWithDeviceLimit = true, OtpChannelKind.SOCIAL)
        }
        is SocialLoginEffect.ShowMessage -> LoginEvent.ShowMessage(message)
    }
}
