package com.vplayed.feature.auth

import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vplayed.core.common.validation.EmailValidationResult
import com.vplayed.core.designsystem.theme.AppTheme
import com.vplayed.feature.auth.presentation.components.LoginTestTags
import com.vplayed.feature.auth.presentation.screen.LoginActions
import com.vplayed.feature.auth.presentation.screen.LoginScreen
import com.vplayed.feature.auth.presentation.viewmodel.LoginUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Login screen layouts and states from fixed UI state (no ViewModel, no network). */
@RunWith(AndroidJUnit4::class)
class LoginScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setScreen(state: LoginUiState = LoginUiState(), size: DpSize = PHONE, actions: LoginActions = LoginActions()) {
        composeRule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(size)) {
                AppTheme { LoginScreen(uiState = state, actions = actions) }
            }
        }
    }

    @Test
    fun givenPhoneWindow_whenShown_thenSingleCardWithoutMarketingColumn() {
        setScreen(size = PHONE)

        composeRule.onNodeWithTag(LoginTestTags.CARD).assertIsDisplayed()
        composeRule.onNodeWithText("Already have an account? Sign in").assertExists()
        assertEquals(0, countWithTag(LoginTestTags.SHOWCASE))
        assertEquals(0, countWithTag(LoginTestTags.GUEST))
    }

    @Test
    fun givenLandscapePhoneWindow_whenShown_thenSingleCardEvenThoughWide() {
        setScreen(size = PHONE_LANDSCAPE)

        composeRule.onNodeWithTag(LoginTestTags.CARD).assertExists()
        assertEquals(0, countWithTag(LoginTestTags.SHOWCASE))
    }

    @Test
    fun givenTabletWindow_whenShown_thenMarketingColumnBesideCardAndGuestButton() {
        setScreen(size = TABLET)

        composeRule.onNodeWithTag(LoginTestTags.SHOWCASE).assertIsDisplayed()
        composeRule.onNodeWithText("Enjoy the Ultimate").assertIsDisplayed()
        composeRule.onNodeWithTag(LoginTestTags.CARD).assertIsDisplayed()
        composeRule.onNodeWithTag(LoginTestTags.GUEST).assertHasClickAction()
    }

    @Test
    fun givenEmptyEmailError_whenShown_thenRequiredMessageReplacesHelp() {
        setScreen(state = LoginUiState(emailError = EmailValidationResult.EMPTY))

        composeRule.onNodeWithText("Email is required").assertIsDisplayed()
        assertEquals(0, countWithText("We'll send a 6-digit code to verify it's you."))
    }

    @Test
    fun givenInvalidEmailError_whenShown_thenInvalidMessageShown() {
        setScreen(state = LoginUiState(email = "user@", emailError = EmailValidationResult.INVALID_FORMAT))
        composeRule.onNodeWithText("Please enter a valid email address").assertIsDisplayed()
    }

    @Test
    fun givenSubmitting_whenTappingContinue_thenButtonIgnoresTaps() {
        var submits = 0
        setScreen(state = LoginUiState(email = "user@example.com", isSubmitting = true), actions = LoginActions(onSubmit = { submits++ }))

        composeRule.onNodeWithTag(LoginTestTags.SUBMIT).assertIsNotEnabled()
        composeRule.onNodeWithTag(LoginTestTags.SUBMIT).performClick()
        assertEquals(0, submits)
    }

    @Test
    fun givenIdle_whenTappingContinueCloseAndGuest_thenCallbacksFire() {
        var submits = 0
        var closes = 0
        setScreen(size = TABLET, actions = LoginActions(onSubmit = { submits++ }, onClose = { closes++ }))

        composeRule.onNodeWithTag(LoginTestTags.SUBMIT).performScrollTo().performClick()
        composeRule.onNodeWithTag(LoginTestTags.GUEST).performScrollTo().performClick()
        composeRule.onNodeWithTag(LoginTestTags.CLOSE).performScrollTo().performClick()

        assertEquals(1, submits)
        assertEquals(2, closes)
    }

    @Test
    fun givenSocialSignIn_whenShown_thenButtonsVisibleButDisabled() {
        setScreen()
        composeRule.onNodeWithText("Sign in with Google").assertIsNotEnabled()
        composeRule.onNodeWithText("Sign in with Apple").assertIsNotEnabled()
    }

    private fun countWithTag(tag: String) = composeRule.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().size

    private fun countWithText(text: String) = composeRule.onAllNodes(hasText(text)).fetchSemanticsNodes().size

    private companion object {
        val PHONE = DpSize(427.dp, 900.dp)
        val PHONE_LANDSCAPE = DpSize(900.dp, 427.dp)
        val TABLET = DpSize(1280.dp, 800.dp)
    }
}
