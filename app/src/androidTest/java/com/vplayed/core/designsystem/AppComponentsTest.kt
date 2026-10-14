package com.vplayed.core.designsystem

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vplayed.core.designsystem.component.AppButton
import com.vplayed.core.designsystem.component.AppDialog
import com.vplayed.core.designsystem.component.AppEmptyState
import com.vplayed.core.designsystem.component.AppErrorView
import com.vplayed.core.designsystem.component.AppLoadingIndicator
import com.vplayed.core.designsystem.component.AppTextField
import com.vplayed.core.designsystem.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppComponentsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun givenEnabledButton_whenClicked_thenInvokesCallback() {
        var clicks = 0
        composeRule.setContent { AppTheme { AppButton(text = "Play", onClick = { clicks++ }) } }

        composeRule.onNodeWithText("Play").assertIsEnabled().performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun givenDisabledButton_whenRendered_thenIsNotEnabled() {
        composeRule.setContent { AppTheme { AppButton(text = "Play", onClick = {}, enabled = false) } }
        composeRule.onNodeWithText("Play").assertIsNotEnabled()
    }

    @Test
    fun givenLoadingButton_whenRendered_thenDisabledAndAnnouncesLoading() {
        composeRule.setContent { AppTheme { AppButton(text = "Save", onClick = {}, loading = true) } }

        composeRule.onNodeWithText("Save").assertIsNotEnabled()
        composeRule.onNode(hasContentDescription("Loading"), useUnmergedTree = true).assertExists()
    }

    @Test
    fun givenErrorMessage_whenRenderingTextField_thenShowsMessageAndErrorSemantics() {
        composeRule.setContent {
            AppTheme { AppTextField(value = "x", onValueChange = {}, label = "Email", errorMessage = "Enter a valid email") }
        }

        composeRule.onNodeWithText("Enter a valid email").assertIsDisplayed()
        composeRule.onNodeWithText("x").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
    }

    @Test
    fun givenErrorView_whenRetryClicked_thenInvokesRetry() {
        var retried = false
        composeRule.setContent { AppTheme { AppErrorView(message = "Network down", onRetry = { retried = true }) } }

        composeRule.onNodeWithText("Network down").assertIsDisplayed()
        composeRule.onNodeWithText("Retry").performClick()

        assertEquals(true, retried)
    }

    @Test
    fun givenEmptyState_whenRendered_thenShowsTitleAndMessage() {
        composeRule.setContent { AppTheme { AppEmptyState(title = "No titles", message = "Add titles to your list") } }

        composeRule.onNodeWithText("No titles").assertIsDisplayed()
        composeRule.onNodeWithText("Add titles to your list").assertIsDisplayed()
    }

    @Test
    fun givenLoadingIndicatorWithMessage_whenRendered_thenDescribedByMessage() {
        composeRule.setContent { AppTheme { AppLoadingIndicator(message = "Loading rails") } }
        composeRule.onNodeWithContentDescription("Loading rails").assertIsDisplayed()
    }

    @Test
    fun givenDialog_whenConfirmClicked_thenInvokesConfirm() {
        var confirmed = false
        composeRule.setContent {
            AppTheme {
                AppDialog(
                    title = "Sign out?",
                    message = "You can sign in again anytime.",
                    confirmLabel = "Sign out",
                    onConfirm = { confirmed = true },
                    onDismiss = {},
                    dismissLabel = "Cancel",
                )
            }
        }

        composeRule.onNodeWithText("Sign out?").assertIsDisplayed()
        composeRule.onNodeWithText("Sign out").performClick()

        assertEquals(true, confirmed)
    }
}
