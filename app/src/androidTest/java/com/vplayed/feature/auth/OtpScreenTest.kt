package com.vplayed.feature.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vplayed.core.designsystem.theme.AppTheme
import com.vplayed.feature.auth.domain.DeviceLimitHandoff
import com.vplayed.feature.auth.presentation.components.LoginTestTags
import com.vplayed.feature.auth.presentation.components.OtpTestTags
import com.vplayed.feature.auth.presentation.screen.OtpActions
import com.vplayed.feature.auth.presentation.screen.OtpScreen
import com.vplayed.feature.auth.presentation.viewmodel.OtpUiState
import com.vplayed.feature.auth.presentation.viewmodel.OtpViewModel
import com.vplayed.shared.auth.AuthMessage
import com.vplayed.shared.auth.AuthMessageKey
import com.vplayed.shared.auth.DeviceKind
import com.vplayed.shared.auth.DeviceSignOutKey
import com.vplayed.shared.auth.OtpStep
import com.vplayed.shared.auth.SignedInDevice
import com.vplayed.shared.session.InMemorySecureStore
import com.vplayed.shared.session.SessionManager
import com.vplayed.testing.FakeAuthRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** OTP screen layouts and behaviour. Static states use fixed UI state; typing uses the real ViewModel with a fake repository. */
@RunWith(AndroidJUnit4::class)
class OtpScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val repository = FakeAuthRepository()

    private fun setStatic(state: OtpUiState, size: DpSize = PHONE, actions: OtpActions = OtpActions()) {
        composeRule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(size)) {
                AppTheme { OtpScreen(uiState = state, actions = actions) }
            }
        }
    }

    /** The real ViewModel drives the screen. Its coroutines use Dispatchers.Main, as in the app. */
    private fun setLive(size: DpSize = PHONE): () -> OtpViewModel {
        lateinit var viewModel: OtpViewModel
        composeRule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(size)) {
                AppTheme {
                    viewModel = remember {
                        OtpViewModel(
                            SavedStateHandle(mapOf(OtpViewModel.ARG_EMAIL to EMAIL, OtpViewModel.ARG_START_WITH_DEVICE_LIMIT to false)),
                            repository,
                            DeviceLimitHandoff(),
                            SessionManager(InMemorySecureStore()),
                        )
                    }
                    val state by viewModel.uiState.collectAsStateWithLifecycle()
                    OtpScreen(
                        uiState = state,
                        actions = OtpActions(
                            onCodeChange = viewModel::onCodeChange,
                            onVerify = viewModel::onVerify,
                            onResend = viewModel::onResend,
                            onBack = viewModel::onBack,
                        ),
                    )
                }
            }
        }
        return { viewModel }
    }

    private fun countWithTag(tag: String) = composeRule.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().size

    @Test
    fun givenPhoneWindow_whenShown_thenCodeStepInSingleCardWithEmailAndDisabledVerify() {
        setStatic(OtpUiState(email = EMAIL, resendSecondsLeft = 42))

        composeRule.onNodeWithText("Verify it's you").assertIsDisplayed()
        composeRule.onNodeWithText("Enter the 6-digit code we sent to $EMAIL").assertIsDisplayed()
        composeRule.onNodeWithTag(OtpTestTags.CODE_FIELD).assertIsDisplayed()
        composeRule.onNodeWithTag(OtpTestTags.VERIFY).assertIsNotEnabled()
        composeRule.onNodeWithTag(OtpTestTags.BACK).assertIsDisplayed()
        assertEquals(0, countWithTag(LoginTestTags.SHOWCASE))
    }

    @Test
    fun givenTabletWindow_whenShown_thenMarketingColumnBesideCodeCard() {
        setStatic(OtpUiState(email = EMAIL), size = TABLET)

        composeRule.onNodeWithTag(LoginTestTags.SHOWCASE).assertIsDisplayed()
        composeRule.onNodeWithTag(OtpTestTags.CARD).assertIsDisplayed()
        composeRule.onNodeWithTag(OtpTestTags.CODE_FIELD).assertIsDisplayed()
    }

    @Test
    fun givenCountdownRunning_whenShown_thenResendDimmedWithRemainingTime() {
        setStatic(OtpUiState(email = EMAIL, resendSecondsLeft = 42))

        composeRule.onNodeWithText("in 0:42").assertIsDisplayed()
        composeRule.onNodeWithTag(OtpTestTags.RESEND).assertIsNotEnabled()
    }

    @Test
    fun givenCountdownFinished_whenTappingResend_thenResendCallbackFires() {
        var resends = 0
        setStatic(OtpUiState(email = EMAIL, resendSecondsLeft = 0), actions = OtpActions(onResend = { resends++ }))

        assertEquals(0, countWithTag(OtpTestTags.RESEND_COUNTDOWN))
        composeRule.onNodeWithTag(OtpTestTags.RESEND).performScrollTo().assertIsEnabled().performClick()

        assertEquals(1, resends)
    }

    @Test
    fun givenSixDigitsTyped_whenShown_thenVerifyEnabledAndTapsVerify() {
        val viewModel = setLive()

        composeRule.onNodeWithTag(OtpTestTags.CODE_FIELD).performTextInput("123456")
        composeRule.onNodeWithTag(OtpTestTags.VERIFY).performScrollTo().assertIsEnabled().performClick()

        composeRule.waitUntil(5_000) { repository.verifications.isNotEmpty() }
        assertEquals(listOf(EMAIL to "123456"), repository.verifications)
        assertEquals(OtpStep.Success, viewModel().uiState.value.step)
    }

    @Test
    fun givenPastedTextWithSeparators_whenEntered_thenOnlySixDigitsAreKept() {
        val viewModel = setLive()

        composeRule.onNodeWithTag(OtpTestTags.CODE_FIELD).performTextInput("12-34 56 78")

        composeRule.waitForIdle()
        assertEquals("123456", viewModel().uiState.value.code)
        composeRule.onNodeWithTag(OtpTestTags.VERIFY).performScrollTo().assertIsEnabled()
    }

    @Test
    fun givenFiveDigits_whenTappingVerify_thenNothingIsSent() {
        setLive()

        composeRule.onNodeWithTag(OtpTestTags.CODE_FIELD).performTextInput("12345")
        composeRule.onNodeWithTag(OtpTestTags.VERIFY).performScrollTo().assertIsNotEnabled().performClick()

        composeRule.waitForIdle()
        assertTrue(repository.verifications.isEmpty())
    }

    @Test
    fun givenDeviceStep_whenShown_thenDevicesListedWithSignOutActions() {
        var signedOut: SignedInDevice? = null
        var allSignedOut = 0
        setStatic(
            OtpUiState(email = EMAIL, step = OtpStep.Devices, devices = devices),
            actions = OtpActions(onSignOutDevice = { signedOut = it }, onSignOutAllDevices = { allSignedOut++ }),
        )

        composeRule.onNodeWithText("Maximum devices reached").assertIsDisplayed()
        composeRule.onNodeWithText("You're already signed in on 2 devices. Sign out from one to continue, or sign out from all of them at once.").assertIsDisplayed()
        composeRule.onNodeWithText("Chrome on Linux").assertIsDisplayed()
        composeRule.onNodeWithText("This device · Chrome").assertIsDisplayed()
        composeRule.onNodeWithTag(OtpTestTags.DEVICE_SIGN_OUT_PREFIX + 1).performScrollTo().performClick()
        composeRule.onNodeWithTag(OtpTestTags.DEVICES_PRIMARY).performScrollTo().performClick()

        assertEquals(devices[1], signedOut)
        assertEquals(1, allSignedOut)
    }

    @Test
    fun givenDeviceStepAfterSignOut_whenShown_thenPrimaryButtonContinues() {
        var continued = 0
        setStatic(
            OtpUiState(email = EMAIL, step = OtpStep.Devices, devices = devices.take(1), hasSignedOutDevice = true),
            actions = OtpActions(onContinueAfterSignOut = { continued++ }),
        )

        composeRule.onNode(hasTestTag(OtpTestTags.DEVICES_PRIMARY) and hasText("Continue")).performScrollTo().performClick()

        assertEquals(1, continued)
    }

    @Test
    fun givenRefusedCode_whenShown_thenInlineErrorUnderBoxesAndNoToastNeeded() {
        setStatic(OtpUiState(email = EMAIL, error = AuthMessage.Localized(AuthMessageKey.OTP_INVALID), shakeCount = 1))

        composeRule.onNodeWithTag(OtpTestTags.ERROR).assertIsDisplayed()
        composeRule.onNodeWithText("Incorrect code. Try again.").assertIsDisplayed()
        composeRule.onNodeWithTag(OtpTestTags.VERIFY).assertIsNotEnabled()
    }

    @Test
    fun givenNoError_whenShown_thenNoErrorLine() {
        setStatic(OtpUiState(email = EMAIL))
        assertEquals(0, countWithTag(OtpTestTags.ERROR))
    }

    @Test
    fun givenServerMessage_whenShown_thenItIsShownInlineAsIs() {
        setStatic(OtpUiState(email = EMAIL, error = AuthMessage.Server("OTP has expired")))
        composeRule.onNodeWithText("OTP has expired").assertIsDisplayed()
    }

    @Test
    fun givenDeviceStepWithError_whenShown_thenInlineErrorAboveButton() {
        setStatic(OtpUiState(email = EMAIL, step = OtpStep.Devices, devices = devices, error = AuthMessage.Server("Could not sign out")))
        composeRule.onNodeWithTag(OtpTestTags.ERROR).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun givenNewUserNotice_whenSuccessShown_thenNoticeIsDisplayed() {
        setStatic(OtpUiState(email = EMAIL, step = OtpStep.Success, notice = AuthMessage.Localized(AuthMessageKey.PROFILE_SETUP_PENDING)))
        composeRule.onNodeWithTag(OtpTestTags.NOTICE).assertIsDisplayed()
    }

    @Test
    fun givenSuccessStep_whenShown_thenAllSetMessageIsDisplayed() {
        setStatic(OtpUiState(email = EMAIL, step = OtpStep.Success))

        composeRule.onNodeWithTag(OtpTestTags.SUCCESS).assertIsDisplayed()
        composeRule.onNodeWithText("You're all set").assertIsDisplayed()
        composeRule.onNodeWithText("Taking you in…").assertIsDisplayed()
    }

    private val devices = listOf(
        SignedInDevice("Chrome on Linux", DeviceKind.DESKTOP, "Chrome", true, DeviceSignOutKey("u1", "t1")),
        SignedInDevice("iPhone 15", DeviceKind.PHONE, null, false, DeviceSignOutKey("u2", "t2")),
    )

    private companion object {
        const val EMAIL = "user@example.com"
        val PHONE = DpSize(427.dp, 900.dp)
        val TABLET = DpSize(1280.dp, 800.dp)
    }
}
