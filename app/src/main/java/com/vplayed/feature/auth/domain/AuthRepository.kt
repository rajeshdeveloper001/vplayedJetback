package com.vplayed.feature.auth.domain

import com.vplayed.core.error.AppError
import com.vplayed.feature.auth.domain.model.DeviceLimit
import com.vplayed.feature.auth.domain.model.DeviceSignOutKey
import com.vplayed.feature.auth.domain.model.OtpVerificationResult
import com.vplayed.feature.auth.domain.model.SignOutDeviceResult

/** Outcome of asking the backend for an email login code. */
sealed interface EmailLoginResult {
    /** HTTP 200 (existing account) or 203 (new account): the backend sent a code to the email. */
    data class CodeSent(val message: String?, val isNewUser: Boolean) : EmailLoginResult

    /** HTTP 202: the account is signed in on the maximum number of devices. */
    data class DeviceLimitReached(val message: String?, val limit: DeviceLimit? = null) : EmailLoginResult

    /** The backend refused the request and may have explained why in [message]. */
    data class Rejected(val message: String?) : EmailLoginResult

    /** No usable answer (network, server error without message, unreadable response, configuration). */
    data class Failure(val error: AppError) : EmailLoginResult
}

/** Email login against the VPlayed backend. Every call returns a result; none throws. */
interface AuthRepository {
    /** Requests (or re-requests) a login code for [email] (trimmed by the implementation). */
    suspend fun requestEmailLogin(email: String): EmailLoginResult

    /** Verifies [code] for [email]. On success the session is stored before [OtpVerificationResult.Verified] is returned. */
    suspend fun verifyEmailOtp(email: String, code: String): OtpVerificationResult

    /** Signs a device out of the account ([all] = every device, using [key] of the first). */
    suspend fun signOutDevice(key: DeviceSignOutKey, all: Boolean): SignOutDeviceResult

    /** Completes the login for [email] after devices were signed out (no code, as in the previous app). */
    suspend fun continueAfterDeviceSignOut(email: String): OtpVerificationResult
}
