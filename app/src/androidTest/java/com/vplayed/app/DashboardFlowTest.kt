package com.vplayed.app

import android.content.pm.ActivityInfo
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vplayed.feature.auth.presentation.components.LoginTestTags
import com.vplayed.feature.auth.presentation.components.OtpTestTags
import com.vplayed.feature.dashboard.home.presentation.components.HomeTestTags
import com.vplayed.shared.auth.DeviceKind
import com.vplayed.shared.auth.DeviceLimit
import com.vplayed.shared.auth.DeviceSignOutKey
import com.vplayed.shared.auth.EmailLoginResult
import com.vplayed.shared.auth.OtpVerificationResult
import com.vplayed.shared.auth.SignedInDevice
import com.vplayed.testing.FakeAuthRepository
import com.vplayed.testing.FakeHomeRepository
import com.vplayed.testing.FakeHomeRepository.Companion.HERO_TITLE
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end user flows on the real app (navigation, ViewModels, DataStore) with the Home API
 * replaced by [FakeHomeRepository]. Each test reads as the steps a tester would follow by hand.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class DashboardFlowTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createEmptyComposeRule()

    @Inject
    lateinit var homeData: FakeHomeRepository

    @Inject
    lateinit var authData: FakeAuthRepository

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    private fun launchApp(): ActivityScenario<MainActivity> = ActivityScenario.launch(MainActivity::class.java)

    private fun waitForText(text: String, timeoutMillis: Long = 5_000) {
        composeRule.waitUntil(timeoutMillis) {
            composeRule.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForTag(tag: String, timeoutMillis: Long = 5_000) {
        composeRule.waitUntil(timeoutMillis) {
            composeRule.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** Home → menu button → Login (the Login screen is a Fragment inside the NavHost). */
    private fun openLoginFromMenu() {
        waitForText(HERO_TITLE)
        composeRule.onNodeWithContentDescription("Open menu").performClick()
        waitForTag(AppDrawerTestTags.LOGIN)
        composeRule.onNodeWithTag(AppDrawerTestTags.LOGIN).performClick()
        waitForTag(LoginTestTags.SCREEN)
    }

    private fun tab(label: String): SemanticsNodeInteraction =
        composeRule.onNode(hasText(label) and hasClickAction() and SemanticsMatcher.keyIsDefined(androidx.compose.ui.semantics.SemanticsProperties.Selected))

    @Test
    fun givenAppLaunch_whenSplashFinishes_thenUserSeesHeroRowsAndHomeTab() {
        launchApp().use {
            waitForText(HERO_TITLE)

            composeRule.onNodeWithText(HERO_TITLE).assertIsDisplayed()
            composeRule.onNodeWithText("Watch now").assertIsDisplayed()
            composeRule.onNodeWithText("Trending Now").assertIsDisplayed()
            tab("Home").assertIsSelected()
        }
    }

    @Test
    fun givenHome_whenUserScrollsToTheBottom_thenNextPagesOfRowsLoad() {
        launchApp().use {
            waitForText(HERO_TITLE)

            repeat(MAX_SWIPES) {
                if (composeRule.onAllNodes(hasText("Latest Movies")).fetchSemanticsNodes().isEmpty()) {
                    composeRule.onNodeWithTag(HomeTestTags.LIST).performTouchInput { swipeUp() }
                    composeRule.waitForIdle()
                }
            }

            composeRule.onNodeWithText("Latest Movies").assertIsDisplayed()
            assertTrue("Pages 2 and 3 were requested", homeData.sectionCalls.containsAll(listOf(2, 3)))
        }
    }

    @Test
    fun givenHome_whenUserVisitsEveryTab_thenEachOpensAndHomeComesBack() {
        launchApp().use {
            waitForText(HERO_TITLE)

            tab("Search").performClick()
            composeRule.onNodeWithText("Search is on its way").assertIsDisplayed()
            tab("Search").assertIsSelected()

            tab("Promos").performClick()
            composeRule.onNodeWithText("Promos are on their way").assertIsDisplayed()

            tab("Subscription").performClick()
            composeRule.onNodeWithText("Plans are on their way").assertIsDisplayed()

            tab("Home").performClick()
            composeRule.onNodeWithText(HERO_TITLE).assertIsDisplayed()
        }
    }

    @Test
    fun givenNoConnection_whenUserTapsRetry_thenHomeLoads() {
        homeData.simulateOffline()

        launchApp().use {
            waitForText("Retry")
            composeRule.onNodeWithText("You're offline. Check your connection and try again.").assertIsDisplayed()

            composeRule.onNodeWithText("Retry").performClick()

            waitForText(HERO_TITLE)
            composeRule.onNodeWithText("Trending Now").assertIsDisplayed()
        }
    }

    @Test
    fun givenHome_whenUserChoosesLightThemeInAppearance_thenSelectionIsKeptAndBackReturnsHome() {
        launchApp().use {
            waitForText(HERO_TITLE)

            composeRule.onNodeWithContentDescription("Appearance settings").performClick()
            composeRule.onNode(hasText("Light") and hasClickAction()).performClick()
            composeRule.onNode(hasText("Light") and hasClickAction()).assertIsSelected()

            composeRule.onNodeWithContentDescription("Navigate back").performClick()
            composeRule.onNodeWithText(HERO_TITLE).assertIsDisplayed()
            tab("Home").assertIsSelected()
        }
    }

    @Test
    fun givenHome_whenUserRotatesToLandscape_thenSameScreenKeepsContent() {
        launchApp().use { scenario ->
            waitForText(HERO_TITLE)
            lateinit var before: MainActivity
            scenario.onActivity { before = it }

            scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
            composeRule.waitForIdle()

            scenario.onActivity { assertSame("Rotation must not recreate the activity", before, it) }
            composeRule.onNodeWithText(HERO_TITLE).assertIsDisplayed()
            tab("Search").assertIsDisplayed()
            scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
        }
    }

    @Test
    fun givenHome_whenUserChoosesLoginInSideMenu_thenLoginScreenOpens() {
        launchApp().use {
            openLoginFromMenu()

            composeRule.onNodeWithText("Sign in or create your account").assertIsDisplayed()
            composeRule.onNodeWithText("Email ID").assertIsDisplayed()
            composeRule.onNodeWithTag(LoginTestTags.SUBMIT).assertIsDisplayed()
        }
    }

    @Test
    fun givenLogin_whenUserTapsClose_thenDashboardIsBack() {
        launchApp().use {
            openLoginFromMenu()

            composeRule.onNodeWithTag(LoginTestTags.CLOSE).performClick()

            waitForText(HERO_TITLE)
            tab("Home").assertIsSelected()
        }
    }

    @Test
    fun givenLogin_whenUserSubmitsInvalidEmail_thenErrorShownAndNothingSent() {
        launchApp().use {
            openLoginFromMenu()

            composeRule.onNodeWithTag(LoginTestTags.EMAIL_FIELD).performTextInput("user@example")
            composeRule.onNodeWithTag(LoginTestTags.SUBMIT).performClick()

            composeRule.onNodeWithText("Please enter a valid email address").assertIsDisplayed()
            assertTrue(authData.requestedEmails.isEmpty())
        }
    }

    @Test
    fun givenLogin_whenUserSubmitsValidEmail_thenOneRequestIsSentAndOtpScreenOpens() {
        launchApp().use {
            openLoginFromMenu()

            submitEmail("user@example.com")

            waitForTag(OtpTestTags.SCREEN)
            assertTrue(authData.requestedEmails == listOf("user@example.com"))
            composeRule.onNodeWithText("Verify it's you").assertIsDisplayed()
            composeRule.onNodeWithText("Enter the 6-digit code we sent to user@example.com").assertIsDisplayed()
        }
    }

    @Test
    fun givenLoginRejectedByBackend_whenUserSubmits_thenStaysOnLoginWithoutOtp() {
        authData.result = EmailLoginResult.Rejected("Account blocked")
        launchApp().use {
            openLoginFromMenu()

            submitEmail("user@example.com")

            composeRule.waitUntil(5_000) { authData.requestedEmails.isNotEmpty() }
            composeRule.waitForIdle()
            composeRule.onNodeWithTag(LoginTestTags.SUBMIT).assertIsDisplayed()
            assertTrue(composeRule.onAllNodes(hasTestTag(OtpTestTags.SCREEN)).fetchSemanticsNodes().isEmpty())
        }
    }

    @Test
    fun givenOtpScreen_whenUserEntersCorrectCode_thenSuccessThenDashboardWithSignedInMenu() {
        launchApp().use {
            openLoginFromMenu()
            submitEmail("user@example.com")
            waitForTag(OtpTestTags.SCREEN)

            composeRule.onNodeWithTag(OtpTestTags.CODE_FIELD).performTextInput("123456")
            composeRule.onNodeWithTag(OtpTestTags.VERIFY).performClick()

            waitForTag(OtpTestTags.SUCCESS)
            composeRule.onNodeWithText("You're all set").assertIsDisplayed()
            assertTrue(authData.verifications == listOf("user@example.com" to "123456"))
            // The success step lasts 3 s, then the Dashboard opens.
            waitForText(HERO_TITLE, timeoutMillis = 10_000)
            tab("Home").assertIsSelected()
            composeRule.onNodeWithContentDescription("Open menu").performClick()
            waitForTag(AppDrawerTestTags.SIGNED_IN)
            composeRule.onNodeWithText("Signed in as user@example.com").assertIsDisplayed()
            assertTrue(composeRule.onAllNodes(hasTestTag(AppDrawerTestTags.LOGIN)).fetchSemanticsNodes().isEmpty())
        }
    }

    @Test
    fun givenOtpScreen_whenUserEntersWrongCode_thenStaysOnOtpWithoutSession() {
        authData.verifyResult = OtpVerificationResult.Rejected("OTP is invalid")
        launchApp().use {
            openLoginFromMenu()
            submitEmail("user@example.com")
            waitForTag(OtpTestTags.SCREEN)

            composeRule.onNodeWithTag(OtpTestTags.CODE_FIELD).performTextInput("000000")
            composeRule.onNodeWithTag(OtpTestTags.VERIFY).performClick()

            composeRule.waitUntil(5_000) { authData.verifications.isNotEmpty() }
            waitForTag(OtpTestTags.ERROR)
            // Inline error as in the prototype (no Toast), still on the OTP screen.
            composeRule.onNodeWithText("OTP is invalid").assertIsDisplayed()
            composeRule.onNodeWithText("Verify it's you").assertIsDisplayed()
            assertTrue(composeRule.onAllNodes(hasTestTag(OtpTestTags.SUCCESS)).fetchSemanticsNodes().isEmpty())
        }
    }

    @Test
    fun givenOtpScreen_whenUserTapsChange_thenLoginIsBackWithEmailKept() {
        launchApp().use {
            openLoginFromMenu()
            submitEmail("user@example.com")
            waitForTag(OtpTestTags.SCREEN)

            composeRule.onNodeWithTag(OtpTestTags.CHANGE).performClick()

            waitForTag(LoginTestTags.SCREEN)
            composeRule.onNodeWithText("user@example.com").assertIsDisplayed()
        }
    }

    @Test
    fun givenOtpScreen_whenUserTapsClose_thenDashboardIsBackSignedOut() {
        launchApp().use {
            openLoginFromMenu()
            submitEmail("user@example.com")
            waitForTag(OtpTestTags.SCREEN)

            composeRule.onNodeWithTag(LoginTestTags.CLOSE).performClick()

            waitForText(HERO_TITLE)
            tab("Home").assertIsSelected()
        }
    }

    @Test
    fun givenAccountAtDeviceLimit_whenUserSignsOutADeviceAndContinues_thenCodeStepOpens() {
        authData.result = EmailLoginResult.DeviceLimitReached("Limit", deviceLimit)
        launchApp().use {
            openLoginFromMenu()
            submitEmail("user@example.com")
            waitForTag(OtpTestTags.DEVICES)
            composeRule.onNodeWithText("Maximum devices reached").assertIsDisplayed()
            composeRule.onNodeWithText("Chrome on Linux").assertIsDisplayed()
            composeRule.onNodeWithText("iPhone 15").assertIsDisplayed()

            authData.result = EmailLoginResult.CodeSent("Sent", isNewUser = false)
            composeRule.onNodeWithTag(OtpTestTags.DEVICE_SIGN_OUT_PREFIX + 0).performClick()
            composeRule.waitUntil(5_000) { composeRule.onAllNodesWithText("Continue").fetchSemanticsNodes().isNotEmpty() }
            composeRule.onNodeWithTag(OtpTestTags.DEVICES_PRIMARY).performClick()

            waitForText("Verify it's you")
            composeRule.onNodeWithTag(OtpTestTags.VERIFY).assertIsDisplayed()
        }
    }

    private fun submitEmail(email: String) {
        composeRule.onNodeWithTag(LoginTestTags.EMAIL_FIELD).performTextInput(email)
        composeRule.onNodeWithTag(LoginTestTags.SUBMIT).performClick()
    }

    private val deviceLimit = DeviceLimit(
        devices = listOf(
            SignedInDevice("Chrome on Linux", DeviceKind.DESKTOP, "Chrome", false, DeviceSignOutKey("u1", "t1")),
            SignedInDevice("iPhone 15", DeviceKind.PHONE, null, false, DeviceSignOutKey("u2", "t2")),
        ),
        maxDevices = 2,
    )

    private companion object {
        const val MAX_SWIPES = 12
    }
}
