package com.vplayed.integration

import androidx.lifecycle.SavedStateHandle
import com.vplayed.core.common.validation.EmailValidationResult
import com.vplayed.core.common.validation.EmailValidator
import com.vplayed.feature.auth.domain.DeviceLimitHandoff
import com.vplayed.feature.auth.presentation.viewmodel.OtpViewModel
import com.vplayed.testing.FakeAuthRepository
import com.vplayed.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Automated test suite implementing the complete test specifications defined in:
 * `docs/testing/specs/input-validation-test-suite.yaml`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class InputValidationSuiteTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // =========================================================================
    // 1. Email ID Input Validation Tests (from YAML: emailFieldValidation)
    // =========================================================================

    @Test
    fun givenYamlEmptyAndWhitespaceTestCases_whenValidated_thenExpectEmptyResult() {
        // EMAIL_VAL_01: Direct empty submission
        assertEquals(EmailValidationResult.EMPTY, EmailValidator.validate(""))

        // EMAIL_VAL_02: Multiple spaces
        assertEquals(EmailValidationResult.EMPTY, EmailValidator.validate("    "))

        // EMAIL_VAL_03: Tab and newline whitespace characters
        assertEquals(EmailValidationResult.EMPTY, EmailValidator.validate("\t\n  \r\n"))
    }

    @Test
    fun givenYamlMalformedEmailCases_whenValidated_thenExpectInvalidFormatResult() {
        val malformedCases = listOf(
            "plainaddress",                              // EMAIL_VAL_04: Missing @ and domain
            "user@",                                     // EMAIL_VAL_05: Missing domain
            "@example.com",                              // EMAIL_VAL_06: Missing local part
            "user@example",                              // EMAIL_VAL_07: Missing TLD
            "user@example.c",                            // EMAIL_VAL_08: TLD < 2 characters
            "us er@example.com",                         // EMAIL_VAL_09: Embedded space in local part
            "user@exam ple.com",                         // EMAIL_VAL_10: Embedded space in domain
            "user@@example.com",                         // EMAIL_VAL_11: Duplicate @
            "user@.com",                                 // EMAIL_VAL_12: Missing domain name before TLD
            "<script>alert(1)</script>@example.com",     // EMAIL_VAL_13: XSS payload
            "admin' OR '1'='1@example.com",              // EMAIL_VAL_14: SQLi payload
        )

        malformedCases.forEach { input ->
            assertEquals("Failed for input: '$input'", EmailValidationResult.INVALID_FORMAT, EmailValidator.validate(input))
        }
    }

    @Test
    fun givenYamlValidEmailCases_whenValidated_thenExpectValidResult() {
        val validCases = listOf(
            "user@example.com",                          // EMAIL_VAL_15: Standard valid email
            "  user@example.com  ",                      // EMAIL_VAL_16: Leading/trailing whitespace
            "first.last+tag@sub.example.co.in",          // EMAIL_VAL_17: Dots, +, subdomains, compound TLD
            "a_b%c-d@example.io",                        // EMAIL_VAL_18: Allowable special characters
            "USER@DOMAIN.COM",                           // EMAIL_VAL_19: Uppercase email
        )

        validCases.forEach { input ->
            assertEquals("Failed for valid input: '$input'", EmailValidationResult.VALID, EmailValidator.validate(input))
        }
    }

    // =========================================================================
    // 2. OTP Code Input Validation Tests (from YAML: otpFieldValidation)
    // =========================================================================

    private fun createOtpViewModel(email: String = "user@example.com"): OtpViewModel {
        val handle = SavedStateHandle(mapOf(OtpViewModel.ARG_EMAIL to email))
        return OtpViewModel(handle, FakeAuthRepository(), DeviceLimitHandoff())
    }

    @Test
    fun givenIncompleteOtpCode_whenUpdating_thenVerifyIsDisabled() {
        val viewModel = createOtpViewModel()

        // OTP_VAL_01: Empty state
        assertEquals("", viewModel.uiState.value.code)
        assertFalse(viewModel.uiState.value.canVerify)

        // OTP_VAL_02: 1 digit
        viewModel.onCodeChange("1")
        assertEquals("1", viewModel.uiState.value.code)
        assertFalse(viewModel.uiState.value.canVerify)

        // OTP_VAL_03: 5 digits
        viewModel.onCodeChange("12345")
        assertEquals("12345", viewModel.uiState.value.code)
        assertFalse(viewModel.uiState.value.canVerify)
    }

    @Test
    fun givenExactSixDigits_whenUpdating_thenVerifyIsEnabled() {
        val viewModel = createOtpViewModel()

        // OTP_VAL_04: 6 digits
        viewModel.onCodeChange("123456")
        assertEquals("123456", viewModel.uiState.value.code)
        assertTrue(viewModel.uiState.value.canVerify)
    }

    @Test
    fun givenMoreThanSixDigits_whenUpdating_thenCodeIsTruncatedToSix() {
        val viewModel = createOtpViewModel()

        // OTP_VAL_05: 9 digits -> truncated to 6
        viewModel.onCodeChange("123456789")
        assertEquals("123456", viewModel.uiState.value.code)
        assertTrue(viewModel.uiState.value.canVerify)
    }

    @Test
    fun givenFormattedOrPastedCode_whenUpdating_thenStripsNonDigits() {
        val viewModel = createOtpViewModel()

        // OTP_VAL_06: Pasted code with dashes and spaces
        viewModel.onCodeChange("12-34 56")
        assertEquals("123456", viewModel.uiState.value.code)
        assertTrue(viewModel.uiState.value.canVerify)

        // OTP_VAL_07: Pasted SMS autofill string
        viewModel.onCodeChange("code: 849201 for login")
        assertEquals("849201", viewModel.uiState.value.code)
        assertTrue(viewModel.uiState.value.canVerify)

        // OTP_VAL_08: Pasted special characters only
        viewModel.onCodeChange("abc!@#$%")
        assertEquals("", viewModel.uiState.value.code)
        assertFalse(viewModel.uiState.value.canVerify)
    }

    // =========================================================================
    // 3. Resend Countdown Timer Validation (from YAML: resendTimerValidation)
    // =========================================================================

    @Test
    fun givenOtpScreenLaunch_whenTimerRuns_thenTransitionsFromDisabledToEnabled() = runTest {
        val viewModel = createOtpViewModel()

        // RESEND_VAL_01: Immediate start state (60s countdown active)
        assertEquals(60, viewModel.uiState.value.resendSecondsLeft)
        assertFalse(viewModel.uiState.value.canResend)

        // RESEND_VAL_02: Mid-countdown state at 30 seconds
        advanceTimeBy(30_000)
        assertEquals(30, viewModel.uiState.value.resendSecondsLeft)
        assertFalse(viewModel.uiState.value.canResend)

        // RESEND_VAL_03: Timer expiration at 60 seconds
        advanceTimeBy(30_000)
        assertEquals(0, viewModel.uiState.value.resendSecondsLeft)
        assertTrue(viewModel.uiState.value.canResend)
    }
}
