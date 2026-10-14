package com.vplayed.reference.showcase

import com.vplayed.core.common.config.AppConfig
import com.vplayed.core.common.config.AppEnvironment
import com.vplayed.core.common.validation.EmailValidationResult
import com.vplayed.core.common.validation.EmailValidator
import com.vplayed.reference.showcase.presentation.viewmodel.DemoContentState
import com.vplayed.reference.showcase.presentation.viewmodel.DesignShowcaseViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DesignShowcaseViewModelTest {

    private val config = AppConfig(
        environment = AppEnvironment.QA,
        serverUrl = "https://api-qags.vplayed.com/",
        imageBaseUrl = "https://images.test/",
        loggingEnabled = true,
        isDebuggable = true,
        versionName = "1.0",
    )
    private val viewModel = DesignShowcaseViewModel(config)

    @Test
    fun givenConfig_whenCreated_thenExposesEnvironmentInfo() {
        val env = viewModel.uiState.value.environment
        assertEquals("qa", env.environmentId)
        assertEquals("https://api-qags.vplayed.com/", env.serverUrl)
    }

    @Test
    fun givenInvalidInput_whenSubmitting_thenShowsValidationError() {
        viewModel.onEmailChange("not-an-email")
        viewModel.onSubmitEmail()

        assertEquals(EmailValidationResult.INVALID_FORMAT, viewModel.uiState.value.emailError)
        assertFalse(viewModel.uiState.value.emailAccepted)
    }

    @Test
    fun givenEmptyInput_whenSubmitting_thenShowsEmptyError() {
        viewModel.onSubmitEmail()
        assertEquals(EmailValidationResult.EMPTY, viewModel.uiState.value.emailError)
    }

    @Test
    fun givenValidationError_whenEditing_thenErrorClears() {
        viewModel.onSubmitEmail()
        viewModel.onEmailChange("a")
        assertNull(viewModel.uiState.value.emailError)
    }

    @Test
    fun givenValidInput_whenSubmitting_thenAcceptsEmail() {
        viewModel.onEmailChange(" viewer@vplayed.com ")
        viewModel.onSubmitEmail()

        assertNull(viewModel.uiState.value.emailError)
        assertTrue(viewModel.uiState.value.emailAccepted)
    }

    @Test
    fun givenDemoState_whenSelected_thenUiStateUpdates() {
        viewModel.onDemoStateSelected(DemoContentState.ERROR)
        assertEquals(DemoContentState.ERROR, viewModel.uiState.value.demoState)
    }

    @Test
    fun givenEmailFormats_whenValidating_thenClassifiesCorrectly() {
        assertEquals(EmailValidationResult.VALID, EmailValidator.validate("a.b+c@example.co"))
        assertEquals(EmailValidationResult.INVALID_FORMAT, EmailValidator.validate("a@b"))
        assertEquals(EmailValidationResult.EMPTY, EmailValidator.validate("   "))
    }
}
