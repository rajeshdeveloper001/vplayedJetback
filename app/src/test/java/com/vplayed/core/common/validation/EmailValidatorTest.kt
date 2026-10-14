package com.vplayed.core.common.validation

import org.junit.Assert.assertEquals
import org.junit.Test

class EmailValidatorTest {

    @Test
    fun givenBlankInput_whenValidating_thenEmpty() {
        assertEquals(EmailValidationResult.EMPTY, EmailValidator.validate(""))
        assertEquals(EmailValidationResult.EMPTY, EmailValidator.validate(" \t "))
    }

    @Test
    fun givenMalformedAddresses_whenValidating_thenInvalidFormat() {
        listOf("user", "user@", "@example.com", "user@example", "user@example.c", "us er@example.com", "user@@example.com")
            .forEach { assertEquals(it, EmailValidationResult.INVALID_FORMAT, EmailValidator.validate(it)) }
    }

    @Test
    fun givenWellFormedAddresses_whenValidating_thenValidAfterTrimming() {
        listOf("user@example.com", "  first.last+tag@sub.example.co.in  ", "a_b%c-d@example.io")
            .forEach { assertEquals(it, EmailValidationResult.VALID, EmailValidator.validate(it)) }
    }
}
