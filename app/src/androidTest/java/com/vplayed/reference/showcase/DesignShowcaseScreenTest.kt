package com.vplayed.reference.showcase

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vplayed.core.common.config.AppConfig
import com.vplayed.core.common.config.AppEnvironment
import com.vplayed.core.designsystem.theme.AppTheme
import com.vplayed.reference.showcase.presentation.screen.DesignShowcaseScreen
import com.vplayed.reference.showcase.presentation.screen.ShowcaseTestTags
import com.vplayed.reference.showcase.presentation.viewmodel.DesignShowcaseViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DesignShowcaseScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val viewModel = DesignShowcaseViewModel(
        AppConfig(AppEnvironment.DEVELOPMENT, "https://api-devgs.vplayed.com/", "https://images.test/", true, true, "1.0"),
    )

    @Composable
    private fun Showcase() {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        AppTheme {
            DesignShowcaseScreen(
                uiState = state,
                onEmailChange = viewModel::onEmailChange,
                onSubmitEmail = viewModel::onSubmitEmail,
                onDemoStateSelected = viewModel::onDemoStateSelected,
            )
        }
    }

    private fun scrollTo(text: String) {
        composeRule.onNodeWithTag(ShowcaseTestTags.GRID).performScrollToNode(hasText(text))
    }

    @Test
    fun givenHome_whenRendered_thenShowsTitleAndEnvironment() {
        composeRule.setContent { Showcase() }

        composeRule.onNodeWithText("Design system").assertIsDisplayed()
        composeRule.onNodeWithText("https://api-devgs.vplayed.com/").assertIsDisplayed()
    }

    @Test
    fun givenInvalidInput_whenSubmitting_thenShowsValidationError() {
        composeRule.setContent { Showcase() }
        scrollTo("Submit")

        composeRule.onNodeWithText("Email").performTextInput("not-an-email")
        composeRule.onNodeWithText("Submit").performClick()

        composeRule.onNodeWithText("Enter a valid email address").assertIsDisplayed()
    }

    @Test
    fun givenStatesCard_whenSwitchingStates_thenShowsLoadingEmptyErrorAndContent() {
        composeRule.setContent { Showcase() }
        scrollTo("Content states")

        composeRule.onNodeWithText("Empty").performClick()
        composeRule.onNodeWithText("Nothing here yet").assertExists()

        composeRule.onNodeWithText("Error").performClick()
        composeRule.onNodeWithText("Check your connection and try again.").assertExists()

        composeRule.onNodeWithText("Retry").performClick()
        composeRule.onNodeWithText("Content loaded successfully.").assertExists()

        composeRule.mainClock.autoAdvance = false // the progress indicator animates forever
        composeRule.onNodeWithText("Loading").performClick()
        composeRule.mainClock.advanceTimeBy(500)
        composeRule.onNodeWithText("Loading content").assertExists()
    }

    @Test
    fun givenTabletWidth_whenRendered_thenSectionsSitSideBySide() {
        composeRule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(1280.dp, 800.dp))) { Showcase() }
        }

        val colorsTop = composeRule.onNodeWithText("Colors").getUnclippedBoundsInRoot().top
        val typeTop = composeRule.onNodeWithText("OTT text styles").getUnclippedBoundsInRoot().top
        assertEquals(colorsTop, typeTop)
    }

    @Test
    fun givenPhoneWidth_whenRendered_thenSectionsStackVertically() {
        composeRule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(400.dp, 2400.dp))) { Showcase() }
        }

        val colorsBounds = composeRule.onNodeWithText("Colors").getUnclippedBoundsInRoot()
        val typeBounds = composeRule.onNodeWithText("OTT text styles").getUnclippedBoundsInRoot()
        assertNotEquals(colorsBounds.top, typeBounds.top)
        assertEquals(colorsBounds.left, typeBounds.left)
    }
}
