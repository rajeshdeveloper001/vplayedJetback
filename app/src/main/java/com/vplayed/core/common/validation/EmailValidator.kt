package com.vplayed.core.common.validation

/*
 * The email rule for every form (login, design-system demo) is the shared one (`../kmp`,
 * `validation/EmailValidator`): local part, '@', domain with a 2–64 letter TLD, at most 50
 * characters, input trimmed first (decision D16, the UIKit app's rule).
 */

typealias EmailValidationResult = com.vplayed.shared.validation.EmailValidationResult

typealias EmailValidator = com.vplayed.shared.validation.EmailValidator
