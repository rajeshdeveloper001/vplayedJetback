package com.vplayed.feature.auth

import androidx.lifecycle.SavedStateHandle
import com.vplayed.R
import com.vplayed.core.error.AppError
import com.vplayed.feature.auth.domain.DeviceLimitHandoff
import com.vplayed.feature.auth.domain.EmailLoginResult
import com.vplayed.feature.auth.domain.model.DeviceKind
import com.vplayed.feature.auth.domain.model.DeviceLimit
import com.vplayed.feature.auth.domain.model.DeviceSignOutKey
import com.vplayed.feature.auth.domain.model.OtpVerificationResult
import com.vplayed.feature.auth.domain.model.SignOutDeviceResult
import com.vplayed.feature.auth.domain.model.SignedInDevice
import com.vplayed.feature.auth.presentation.viewmodel.AuthMessage
import com.vplayed.feature.auth.presentation.viewmodel.OtpEvent
import com.vplayed.feature.auth.presentation.viewmodel.OtpStep
import com.vplayed.feature.auth.presentation.viewmodel.OtpViewModel
import com.vplayed.testing.FakeAuthRepository
import com.vplayed.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

// Virtual time drives the resend countdown and the success delay.
@OptIn(ExperimentalCoroutinesApi::class)
class OtpViewModelTest {

    // Standard dispatcher: coroutines only run when the test lets them.
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    private val repository = FakeAuthRepository()
    private val handoff = DeviceLimitHandoff()

    private val laptop = SignedInDevice("Chrome on Linux", DeviceKind.DESKTOP, "Chrome", false, DeviceSignOutKey("u1", "t1"))
    private val phone = SignedInDevice("iPhone 15", DeviceKind.PHONE, null, false, DeviceSignOutKey("u2", "t2"))

    private fun viewModel(startWithDeviceLimit: Boolean = false) = OtpViewModel(
        SavedStateHandle(mapOf(OtpViewModel.ARG_EMAIL to EMAIL, OtpViewModel.ARG_START_WITH_DEVICE_LIMIT to startWithDeviceLimit)),
        repository,
        handoff,
    )

    private fun TestScope.started(startWithDeviceLimit: Boolean = false): OtpViewModel =
        viewModel(startWithDeviceLimit).also { runCurrent() }

    private suspend fun OtpViewModel.nextEvent(): OtpEvent = events.first()

    private fun OtpViewModel.typeFullCode(code: String = "123456") = onCodeChange(code)

    // region code input

    @Test
    fun givenTyping_whenEnteringCharacters_thenOnlySixDigitsAreKept() = runTest(mainDispatcherRule.testDispatcher) {
        val vm = started()

        vm.onCodeChange("12a-34 56789")

        assertEquals("123456", vm.uiState.value.code)
    }

    @Test
    fun givenPastedCodeWithSpaces_whenChanged_thenDigitsKept() = runTest(mainDispatcherRule.testDispatcher) {
        val vm = started()
        vm.onCodeChange("123 456")
        assertEquals("123456", vm.uiState.value.code)
        assertTrue(vm.uiState.value.canVerify)
    }

    @Test
    fun givenFewerThanSixDigits_whenVerifying_thenNothingIsSent() = runTest(mainDispatcherRule.testDispatcher) {
        val vm = started()
        vm.onCodeChange("12345")

        assertFalse(vm.uiState.value.canVerify)
        vm.onVerify()
        runCurrent()

        assertTrue(repository.verifications.isEmpty())
    }

    // endregion

    // region verify

    @Test
    fun givenCorrectCode_whenVerifying_thenSuccessStepThenAuthenticatedAfterThreeSeconds() = runTest(mainDispatcherRule.testDispatcher) {
        val vm = started()
        vm.typeFullCode()

        vm.onVerify()
        runCurrent()

        assertEquals(listOf(EMAIL to "123456"), repository.verifications)
        assertEquals(OtpStep.Success, vm.uiState.value.step)
        assertFalse(vm.uiState.value.isVerifying)
        advanceTimeBy(OtpViewModel.SUCCESS_MILLIS - 1)
        runCurrent()
        assertEquals(OtpStep.Success, vm.uiState.value.step) // still celebrating, nothing navigated
        advanceTimeBy(2)
        runCurrent()
        assertEquals(OtpEvent.Authenticated, vm.nextEvent())
    }

    @Test
    fun givenIncorrectCode_whenVerifying_thenInlineErrorShakeAndEmptyBoxesLikeThePrototype() = runTest(mainDispatcherRule.testDispatcher) {
        repository.verifyResult = OtpVerificationResult.Rejected("OTP is invalid")
        val vm = started()
        vm.typeFullCode()

        vm.onVerify()
        runCurrent()

        val state = vm.uiState.value
        assertEquals(OtpStep.Code, state.step)
        assertEquals(AuthMessage.Server("OTP is invalid"), state.error)
        assertEquals("", state.code)
        assertEquals(1, state.shakeCount)
        assertFalse(state.isVerifying)
        vm.onCodeChange("1")
        assertEquals("Editing clears the error", null, vm.uiState.value.error)
        assertEquals("The shake counter only ever grows", 1, vm.uiState.value.shakeCount)
    }

    @Test
    fun givenTwoRefusedCodes_whenVerifying_thenShakesEachTime() = runTest(mainDispatcherRule.testDispatcher) {
        repository.verifyResult = OtpVerificationResult.Rejected(null)
        val vm = started()
        repeat(2) {
            vm.typeFullCode()
            vm.onVerify()
            runCurrent()
        }
        assertEquals(2, vm.uiState.value.shakeCount)
    }

    @Test
    fun givenExpiredCodeWithoutMessage_whenVerifying_thenFallbackMessage() = runTest(mainDispatcherRule.testDispatcher) {
        repository.verifyResult = OtpVerificationResult.Rejected(null)
        val vm = started()
        vm.typeFullCode()
        vm.onVerify()
        runCurrent()
        assertEquals(AuthMessage.Resource(R.string.otp_invalid), vm.uiState.value.error)
    }

    @Test
    fun givenNetworkFailure_whenVerifying_thenInlineOfflineErrorWithCodeKeptAndNoShake() = runTest(mainDispatcherRule.testDispatcher) {
        repository.verifyResult = OtpVerificationResult.Failure(AppError.Network)
        val vm = started()
        vm.typeFullCode()

        vm.onVerify()
        runCurrent()

        assertEquals(AuthMessage.Resource(R.string.error_network), vm.uiState.value.error)
        assertEquals("123456", vm.uiState.value.code)
        assertEquals(0, vm.uiState.value.shakeCount)
        assertFalse(vm.uiState.value.isVerifying)
        assertEquals(OtpStep.Code, vm.uiState.value.step)
    }

    @Test
    fun givenInlineError_whenVerifyingAgain_thenErrorIsClearedWhileRetrying() = runTest(mainDispatcherRule.testDispatcher) {
        repository.verifyResult = OtpVerificationResult.Failure(AppError.Network)
        val vm = started()
        vm.typeFullCode()
        vm.onVerify()
        runCurrent()
        assertEquals(AuthMessage.Resource(R.string.error_network), vm.uiState.value.error)

        repository.verifyHold = CompletableDeferred()
        vm.onVerify()
        runCurrent()

        assertEquals(null, vm.uiState.value.error)
    }

    @Test
    fun givenVerificationInFlight_whenVerifyingAgainOrEditing_thenIgnored() = runTest(mainDispatcherRule.testDispatcher) {
        val hold = CompletableDeferred<Unit>()
        repository.verifyHold = hold
        val vm = started()
        vm.typeFullCode()

        vm.onVerify()
        runCurrent()
        assertTrue(vm.uiState.value.isVerifying)
        vm.onVerify()
        vm.onCodeChange("654321")
        runCurrent()

        assertEquals(1, repository.verifications.size)
        assertEquals("123456", vm.uiState.value.code)
        hold.complete(Unit)
        runCurrent()
        assertFalse(vm.uiState.value.isVerifying)
    }

    @Test
    fun givenNewUser_whenVerified_thenSuccessStepTellsWhatIsComingAndStillContinues() = runTest(mainDispatcherRule.testDispatcher) {
        repository.verifyResult = OtpVerificationResult.Verified(isNewUser = true, hasMultipleProfiles = true)
        val vm = started()
        vm.typeFullCode()

        vm.onVerify()
        runCurrent()

        assertEquals(AuthMessage.Resource(R.string.otp_new_user_pending), vm.uiState.value.notice)
        advanceTimeBy(OtpViewModel.SUCCESS_MILLIS + 1)
        runCurrent()
        assertEquals(OtpEvent.Authenticated, vm.nextEvent())
    }

    @Test
    fun givenSeveralProfiles_whenVerified_thenSuccessStepMentionsProfileChoice() = runTest(mainDispatcherRule.testDispatcher) {
        repository.verifyResult = OtpVerificationResult.Verified(isNewUser = false, hasMultipleProfiles = true)
        val vm = started()
        vm.typeFullCode()
        vm.onVerify()
        runCurrent()
        assertEquals(AuthMessage.Resource(R.string.otp_multi_profile_pending), vm.uiState.value.notice)
    }

    @Test
    fun givenPlainLogin_whenVerified_thenNoNotice() = runTest(mainDispatcherRule.testDispatcher) {
        val vm = started()
        vm.typeFullCode()
        vm.onVerify()
        runCurrent()
        assertEquals(null, vm.uiState.value.notice)
    }

    @Test
    fun givenRegistrationRequired_whenVerifying_thenInlineMessageAndNoSuccess() = runTest(mainDispatcherRule.testDispatcher) {
        repository.verifyResult = OtpVerificationResult.RegistrationRequired(null)
        val vm = started()
        vm.typeFullCode()
        vm.onVerify()
        runCurrent()
        assertEquals(AuthMessage.Resource(R.string.otp_registration_pending), vm.uiState.value.error)
        assertEquals(OtpStep.Code, vm.uiState.value.step)
    }

    // endregion

    // region resend + countdown

    @Test
    fun givenScreenOpened_whenTimePasses_thenResendCountsDownFromSixtyThenEnables() = runTest(mainDispatcherRule.testDispatcher) {
        val vm = started()
        assertEquals(60, vm.uiState.value.resendSecondsLeft)
        assertFalse(vm.uiState.value.canResend)

        advanceTimeBy(30_000)
        runCurrent()
        assertEquals(30, vm.uiState.value.resendSecondsLeft)

        advanceTimeBy(30_001)
        runCurrent()
        assertEquals(0, vm.uiState.value.resendSecondsLeft)
        assertTrue(vm.uiState.value.canResend)
    }

    @Test
    fun givenCountdownRunning_whenResending_thenNothingIsRequested() = runTest(mainDispatcherRule.testDispatcher) {
        val vm = started()
        vm.onResend()
        runCurrent()
        assertTrue(repository.requestedEmails.isEmpty())
    }

    @Test
    fun givenCountdownFinished_whenResending_thenNewCodeRequestedBoxesClearedAndCountdownRestartsWithoutAnyMessage() = runTest(mainDispatcherRule.testDispatcher) {
        repository.result = EmailLoginResult.CodeSent("A new code is on its way", isNewUser = false)
        val vm = started()
        vm.onCodeChange("123")
        advanceTimeBy(61_000)
        runCurrent()

        vm.onResend()
        runCurrent()

        assertEquals(listOf(EMAIL), repository.requestedEmails)
        assertEquals("", vm.uiState.value.code)
        assertEquals(60, vm.uiState.value.resendSecondsLeft)
        assertFalse(vm.uiState.value.isResending)
        assertEquals(null, vm.uiState.value.error)
    }

    @Test
    fun givenResendFails_whenResending_thenInlineErrorInputKeptCountdownNotRestartedAndRetryPossible() = runTest(mainDispatcherRule.testDispatcher) {
        repository.result = EmailLoginResult.Rejected("Too many requests")
        val vm = started()
        vm.onCodeChange("123")
        advanceTimeBy(61_000)
        runCurrent()

        vm.onResend()
        runCurrent()

        assertEquals("123", vm.uiState.value.code)
        assertEquals(0, vm.uiState.value.resendSecondsLeft)
        assertTrue(vm.uiState.value.canResend)
        assertEquals(AuthMessage.Server("Too many requests"), vm.uiState.value.error)
        assertEquals(0, vm.uiState.value.shakeCount)
    }

    @Test
    fun givenResendInFlight_whenResendingAgain_thenOnlyOneRequest() = runTest(mainDispatcherRule.testDispatcher) {
        val hold = CompletableDeferred<Unit>()
        repository.hold = hold
        val vm = started()
        advanceTimeBy(61_000)
        runCurrent()

        vm.onResend()
        runCurrent()
        assertTrue(vm.uiState.value.isResending)
        vm.onResend()
        runCurrent()

        assertEquals(1, repository.requestedEmails.size)
        hold.complete(Unit)
        runCurrent()
        assertFalse(vm.uiState.value.isResending)
    }

    @Test
    fun givenVerifying_whenCountdownFinishes_thenResendStaysDisabledUntilDone() = runTest(mainDispatcherRule.testDispatcher) {
        val hold = CompletableDeferred<Unit>()
        repository.verifyHold = hold
        val vm = started()
        vm.typeFullCode()
        advanceTimeBy(61_000)
        runCurrent()

        vm.onVerify()
        runCurrent()

        assertFalse(vm.uiState.value.canResend)
        hold.complete(Unit)
    }

    @Test
    fun givenSuccess_whenViewModelCleared_thenCountdownNoLongerRuns() = runTest(mainDispatcherRule.testDispatcher) {
        val vm = started()
        vm.typeFullCode()
        vm.onVerify()
        runCurrent()
        // Verified: the countdown stops, there is nothing left to resend.
        assertEquals(0, vm.uiState.value.resendSecondsLeft)
        advanceTimeBy(5_000)
        runCurrent()
        assertEquals(0, vm.uiState.value.resendSecondsLeft)
    }

    // endregion

    // region back

    @Test
    fun givenCodeStep_whenGoingBackOrChangingEmail_thenNavigatesBack() = runTest(mainDispatcherRule.testDispatcher) {
        val vm = started()
        vm.onBack()
        runCurrent()
        assertEquals(OtpEvent.NavigateBack, vm.nextEvent())
    }

    @Test
    fun givenVerifying_whenGoingBack_thenIgnored() = runTest(mainDispatcherRule.testDispatcher) {
        repository.verifyHold = CompletableDeferred()
        val vm = started()
        vm.typeFullCode()
        vm.onVerify()
        runCurrent()

        vm.onBack()
        runCurrent()

        assertEquals(OtpStep.Code, vm.uiState.value.step)
        assertTrue(vm.uiState.value.isVerifying)
    }

    // endregion

    // region devices

    private fun deviceLimitFromVerify() {
        repository.verifyResult = OtpVerificationResult.DeviceLimitReached("Limit", DeviceLimit(listOf(laptop, phone), maxDevices = 2))
    }

    @Test
    fun givenDeviceLimit_whenVerifying_thenDeviceStepListsDevicesAndStopsCountdown() = runTest(mainDispatcherRule.testDispatcher) {
        deviceLimitFromVerify()
        val vm = started()
        vm.typeFullCode()

        vm.onVerify()
        runCurrent()

        assertEquals(OtpStep.Devices, vm.uiState.value.step)
        assertEquals(listOf(laptop, phone), vm.uiState.value.devices)
        assertEquals(2, vm.uiState.value.maxDevices)
        assertFalse(vm.uiState.value.hasSignedOutDevice)
    }

    @Test
    fun givenDeviceStepAfterVerify_whenGoingBack_thenCodeStepWithEmptyBoxes() = runTest(mainDispatcherRule.testDispatcher) {
        deviceLimitFromVerify()
        val vm = started()
        vm.typeFullCode()
        vm.onVerify()
        runCurrent()

        vm.onBack()
        runCurrent()

        assertEquals(OtpStep.Code, vm.uiState.value.step)
        assertEquals("", vm.uiState.value.code)
    }

    @Test
    fun givenDeviceStepAfterVerify_whenSigningOutOneDevice_thenItLeavesTheListAndContinueAppears() = runTest(mainDispatcherRule.testDispatcher) {
        deviceLimitFromVerify()
        val vm = started()
        vm.typeFullCode()
        vm.onVerify()
        runCurrent()

        vm.onSignOutDevice(laptop)
        runCurrent()

        assertEquals(listOf(laptop.signOutKey to false), repository.signOuts)
        assertEquals(listOf(phone), vm.uiState.value.devices)
        assertTrue(vm.uiState.value.hasSignedOutDevice)
        assertEquals(OtpStep.Devices, vm.uiState.value.step)
    }

    @Test
    fun givenSignedOutADevice_whenContinuing_thenVerifyEmailLoginIsUsedAndSuccessShown() = runTest(mainDispatcherRule.testDispatcher) {
        deviceLimitFromVerify()
        val vm = started()
        vm.typeFullCode()
        vm.onVerify()
        runCurrent()
        vm.onSignOutDevice(laptop)
        runCurrent()

        vm.onContinueAfterSignOut()
        runCurrent()

        assertEquals(listOf(EMAIL), repository.continuedEmails)
        assertEquals(OtpStep.Success, vm.uiState.value.step)
    }

    @Test
    fun givenNoDeviceSignedOutYet_whenContinuing_thenIgnored() = runTest(mainDispatcherRule.testDispatcher) {
        deviceLimitFromVerify()
        val vm = started()
        vm.typeFullCode()
        vm.onVerify()
        runCurrent()

        vm.onContinueAfterSignOut()
        runCurrent()

        assertTrue(repository.continuedEmails.isEmpty())
    }

    @Test
    fun givenSignOutAll_whenSucceeds_thenFlowContinuesAutomatically() = runTest(mainDispatcherRule.testDispatcher) {
        deviceLimitFromVerify()
        val vm = started()
        vm.typeFullCode()
        vm.onVerify()
        runCurrent()

        vm.onSignOutAllDevices()
        runCurrent()

        assertEquals(listOf(laptop.signOutKey to true), repository.signOuts)
        assertEquals(listOf(EMAIL), repository.continuedEmails)
        assertEquals(OtpStep.Success, vm.uiState.value.step)
    }

    @Test
    fun givenLastDeviceSignedOut_whenSucceeds_thenFlowContinuesAutomatically() = runTest(mainDispatcherRule.testDispatcher) {
        repository.verifyResult = OtpVerificationResult.DeviceLimitReached(null, DeviceLimit(listOf(laptop), maxDevices = 1))
        val vm = started()
        vm.typeFullCode()
        vm.onVerify()
        runCurrent()

        vm.onSignOutDevice(laptop)
        runCurrent()

        assertEquals(OtpStep.Success, vm.uiState.value.step)
    }

    @Test
    fun givenSignOutFails_whenSigningOut_thenDeviceStaysAndInlineErrorShown() = runTest(mainDispatcherRule.testDispatcher) {
        deviceLimitFromVerify()
        val vm = started()
        vm.typeFullCode()
        vm.onVerify()
        runCurrent()
        repository.signOutResult = SignOutDeviceResult.Rejected("Could not sign out")

        vm.onSignOutDevice(laptop)
        runCurrent()

        assertEquals(listOf(laptop, phone), vm.uiState.value.devices)
        assertFalse(vm.uiState.value.isSigningOut)
        assertEquals(AuthMessage.Server("Could not sign out"), vm.uiState.value.error)
    }

    @Test
    fun givenSignOutInFlight_whenSigningOutAnother_thenIgnored() = runTest(mainDispatcherRule.testDispatcher) {
        deviceLimitFromVerify()
        val vm = started()
        vm.typeFullCode()
        vm.onVerify()
        runCurrent()
        val hold = CompletableDeferred<Unit>()
        repository.signOutHold = hold

        vm.onSignOutDevice(laptop)
        runCurrent()
        vm.onSignOutDevice(phone)
        vm.onSignOutAllDevices()
        runCurrent()

        assertEquals(1, repository.signOuts.size)
        hold.complete(Unit)
    }

    @Test
    fun givenDeviceLimitFromEmailLogin_whenOpened_thenDeviceStepFromHandoffWithNoCountdown() = runTest(mainDispatcherRule.testDispatcher) {
        handoff.put(DeviceLimit(listOf(laptop, phone), maxDevices = 2))

        val vm = started(startWithDeviceLimit = true)

        assertEquals(OtpStep.Devices, vm.uiState.value.step)
        assertEquals(listOf(laptop, phone), vm.uiState.value.devices)
        assertEquals(0, vm.uiState.value.resendSecondsLeft)
    }

    @Test
    fun givenDeviceStepFromEmailLogin_whenGoingBack_thenLeavesToEmailStep() = runTest(mainDispatcherRule.testDispatcher) {
        handoff.put(DeviceLimit(listOf(laptop), maxDevices = 1))
        val vm = started(startWithDeviceLimit = true)

        vm.onBack()
        runCurrent()

        assertEquals(OtpEvent.NavigateBack, vm.nextEvent())
    }

    @Test
    fun givenDeviceStepFromEmailLogin_whenContinuingAfterSignOut_thenNewCodeRequestedAndCodeStepShown() = runTest(mainDispatcherRule.testDispatcher) {
        handoff.put(DeviceLimit(listOf(laptop, phone), maxDevices = 2))
        val vm = started(startWithDeviceLimit = true)
        vm.onSignOutDevice(laptop)
        runCurrent()

        vm.onContinueAfterSignOut()
        runCurrent()

        assertEquals(listOf(EMAIL), repository.requestedEmails)
        assertTrue(repository.continuedEmails.isEmpty())
        assertEquals(OtpStep.Code, vm.uiState.value.step)
        assertEquals(60, vm.uiState.value.resendSecondsLeft)
    }

    @Test
    fun givenMissingHandoff_whenOpenedOnDeviceStep_thenFallsBackToCodeStep() = runTest(mainDispatcherRule.testDispatcher) {
        val vm = started(startWithDeviceLimit = true)
        assertEquals(OtpStep.Code, vm.uiState.value.step)
    }

    // endregion

    private companion object {
        const val EMAIL = "user@example.com"
    }
}
