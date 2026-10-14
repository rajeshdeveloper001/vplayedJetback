package com.vplayed.core.common.validation

enum class EmailValidationResult { VALID, EMPTY, INVALID_FORMAT }

/**
 * Email rule shared by every form (login, design-system demo). Same accepted shape as the previous
 * app's login check: local part, '@', domain with a 2+ letter TLD. Input is trimmed first.
 */
object EmailValidator {
    private val pattern = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun validate(input: String): EmailValidationResult {
        val email = input.trim()
        return when {
            email.isEmpty() -> EmailValidationResult.EMPTY
            !pattern.matches(email) -> EmailValidationResult.INVALID_FORMAT
            else -> EmailValidationResult.VALID
        }
    }
}
