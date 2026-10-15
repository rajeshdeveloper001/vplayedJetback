package com.vplayed.feature.auth

import com.vplayed.feature.auth.domain.DeviceLimitHandoff
import com.vplayed.feature.auth.presentation.viewmodel.LoginEvent
import com.vplayed.feature.auth.presentation.viewmodel.LoginViewModel
import com.vplayed.shared.auth.AuthMessage
import com.vplayed.shared.auth.AuthMessageKey
import com.vplayed.shared.auth.DeviceKind
import com.vplayed.shared.auth.DeviceLimit
import com.vplayed.shared.auth.DeviceSignOutKey
import com.vplayed.shared.auth.EmailLoginResult
import com.vplayed.shared.auth.SignedInDevice
import com.vplayed.shared.core.error.AppError
import com.vplayed.shared.validation.EmailValidationResult
import com.vplayed.testing.FakeAuthRepository
import com.vplayed.testing.MainDispatcherRule
import com.vplayed.testing.testAuthSession
import com.vplayed.testing.testSessionManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeAuthRepository()
    private val handoff = DeviceLimitHandoff()
    private val sessions = testSessionManager()
    private lateinit var viewModel: LoginViewModel

    // Created after the dispatcher rule (the ViewModel collects the session in init).
    @Before
    fun setUp() {
        viewModel = LoginViewModel(repository, handoff, sessions)
    }

    private suspend fun nextMessage(): AuthMessage = (viewModel.events.first() as LoginEvent.ShowMessage).message

    private val device = SignedInDevice("Pixel", DeviceKind.PHONE, null, false, DeviceSignOutKey("u", "t"))

    @Test
    fun givenEmptyEmail_whenSubmitting_thenEmptyErrorAndNoRequest() {
        viewModel.onEmailChange("   ")
        viewModel.onSubmit()

        assertEquals(EmailValidationResult.EMPTY, viewModel.uiState.value.emailError)
        assertTrue(repository.requestedEmails.isEmpty())
    }

    @Test
    fun givenMalformedEmail_whenSubmitting_thenInvalidErrorAndNoRequest() {
        viewModel.onEmailChange("user@example")
        viewModel.onSubmit()

        assertEquals(EmailValidationResult.INVALID_FORMAT, viewModel.uiState.value.emailError)
        assertTrue(repository.requestedEmails.isEmpty())
    }

    @Test
    fun givenError_whenEditingEmail_thenErrorCleared() {
        viewModel.onSubmit()
        viewModel.onEmailChange("u")
        assertNull(viewModel.uiState.value.emailError)
    }

    @Test
    fun givenMalformedEmail_whenFieldLosesFocus_thenInvalidErrorShown() {
        viewModel.onEmailChange("user@")
        viewModel.onEmailFocusLost()
        assertEquals(EmailValidationResult.INVALID_FORMAT, viewModel.uiState.value.emailError)
    }

    @Test
    fun givenEmptyEmail_whenFieldLosesFocus_thenNoError() {
        viewModel.onEmailFocusLost()
        assertNull(viewModel.uiState.value.emailError)
    }

    @Test
    fun givenCodeSent_whenSubmitting_thenOtpScreenOpensForTrimmedEmailAndLoadingEnds() = runTest {
        repository.result = EmailLoginResult.CodeSent("OTP sent to your email", isNewUser = false)
        viewModel.onEmailChange("  user@example.com ")

        viewModel.onSubmit()

        assertEquals(LoginEvent.OpenOtp("user@example.com", startWithDeviceLimit = false), viewModel.events.first())
        assertEquals(listOf("  user@example.com "), repository.requestedEmails)
        assertFalse(viewModel.uiState.value.isSubmitting)
    }

    @Test
    fun givenCodeSentToNewUser_whenSubmitting_thenOtpScreenOpens() = runTest {
        repository.result = EmailLoginResult.CodeSent(null, isNewUser = true)
        viewModel.onEmailChange("user@example.com")
        viewModel.onSubmit()
        assertEquals(LoginEvent.OpenOtp("user@example.com", startWithDeviceLimit = false), viewModel.events.first())
    }

    @Test
    fun givenDeviceLimitWithDevices_whenSubmitting_thenOtpOpensOnDeviceStepWithDevicesHandedOver() = runTest {
        val limit = DeviceLimit(listOf(device), maxDevices = 1)
        repository.result = EmailLoginResult.DeviceLimitReached("Limit", limit)
        viewModel.onEmailChange("user@example.com")

        viewModel.onSubmit()

        assertEquals(LoginEvent.OpenOtp("user@example.com", startWithDeviceLimit = true), viewModel.events.first())
        assertEquals(limit, handoff.take())
        assertNull("The handoff is single use", handoff.take())
    }

    @Test
    fun givenDeviceLimitWithoutDevices_whenSubmitting_thenToastAndNoNavigation() = runTest {
        repository.result = EmailLoginResult.DeviceLimitReached(null, null)
        viewModel.onEmailChange("user@example.com")
        viewModel.onSubmit()
        assertEquals(AuthMessage.Localized(AuthMessageKey.DEVICE_LIMIT), nextMessage())
        assertNull(handoff.take())
    }

    @Test
    fun givenRejectedWithMessage_whenSubmitting_thenServerErrorShown() = runTest {
        repository.result = EmailLoginResult.Rejected("Account blocked")
        viewModel.onEmailChange("user@example.com")
        viewModel.onSubmit()
        assertEquals(AuthMessage.Server("Account blocked"), nextMessage())
    }

    @Test
    fun givenRejectedWithUnsafeMessage_whenSubmitting_thenFallbackErrorShown() = runTest {
        repository.result = EmailLoginResult.Rejected("<html><body>Fatal error at line 3</body></html>")
        viewModel.onEmailChange("user@example.com")
        viewModel.onSubmit()
        assertEquals(AuthMessage.Localized(AuthMessageKey.LOGIN_FAILED), nextMessage())
    }

    @Test
    fun givenNetworkFailure_whenSubmitting_thenOfflineErrorShown() = runTest {
        repository.result = EmailLoginResult.Failure(AppError.Network)
        viewModel.onEmailChange("user@example.com")
        viewModel.onSubmit()
        assertEquals(AuthMessage.Localized(AuthMessageKey.ERROR_NETWORK), nextMessage())
        assertFalse(viewModel.uiState.value.isSubmitting)
    }

    @Test
    fun givenTypedEmail_whenLoginClosed_thenEmailForgotten() {
        viewModel.onEmailChange("user@example.com")
        viewModel.onClosed()
        assertEquals("", viewModel.uiState.value.email)
    }

    @Test
    fun givenTypedEmail_whenSignedIn_thenEmailForgotten() = runTest {
        viewModel.onEmailChange("user@example.com")
        sessions.signIn(testAuthSession)
        assertEquals("", viewModel.uiState.value.email)
    }

    @Test
    fun givenRequestInFlight_whenSubmittingAgain_thenSecondSubmitIgnored() = runTest {
        val hold = CompletableDeferred<Unit>()
        repository.hold = hold
        viewModel.onEmailChange("user@example.com")

        viewModel.onSubmit()
        assertTrue(viewModel.uiState.value.isSubmitting)
        viewModel.onSubmit()
        viewModel.onEmailChange("other@example.com")

        assertEquals(1, repository.requestedEmails.size)
        assertEquals("user@example.com", viewModel.uiState.value.email)
        hold.complete(Unit)
        viewModel.events.first()
        assertFalse(viewModel.uiState.value.isSubmitting)
    }
}
