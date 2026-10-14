package com.vplayed.testing

import com.vplayed.shared.auth.AuthRepository
import com.vplayed.shared.auth.AuthSession
import com.vplayed.shared.auth.DeviceSignOutKey
import com.vplayed.shared.auth.EmailLoginResult
import com.vplayed.shared.auth.GuestLoginResult
import com.vplayed.shared.auth.LogoutResult
import com.vplayed.shared.auth.OtpVerificationResult
import com.vplayed.shared.auth.SignOutDeviceResult
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Login without a backend: records each call and answers with the configured results. A successful
 * verify returns a test session for the email; `OtpViewModel` stores it, as with the real repository.
 */
@Singleton
class FakeAuthRepository @Inject constructor() : AuthRepository {
    @Volatile
    var result: EmailLoginResult = EmailLoginResult.CodeSent(message = "Code sent", isNewUser = false)

    /** Null answers "signed in" with [testSession]. */
    @Volatile
    var verifyResult: OtpVerificationResult? = null

    @Volatile
    var signOutResult: SignOutDeviceResult = SignOutDeviceResult.Success

    val requestedEmails: MutableList<String> = Collections.synchronizedList(mutableListOf())
    val verifications: MutableList<Pair<String, String>> = Collections.synchronizedList(mutableListOf())

    override suspend fun requestEmailLogin(email: String): EmailLoginResult {
        requestedEmails += email
        return result
    }

    override suspend fun verifyEmailOtp(email: String, code: String): OtpVerificationResult {
        verifications += email to code
        return answer(email)
    }

    override suspend fun signOutDevice(key: DeviceSignOutKey, all: Boolean): SignOutDeviceResult = signOutResult

    override suspend fun continueAfterDeviceSignOut(email: String): OtpVerificationResult = answer(email)

    // This app has no guest login or logout yet.
    override suspend fun guestLogin(): GuestLoginResult = error("not used by AndroidJP")

    override suspend fun logout(): LogoutResult = error("not used by AndroidJP")

    private fun answer(email: String): OtpVerificationResult =
        verifyResult ?: OtpVerificationResult.Verified(testSession(email.trim()), isNewUser = false, hasMultipleProfiles = false)

    private fun testSession(email: String) = AuthSession(
        accessToken = "test-access-token",
        refreshToken = "test-refresh-token",
        userId = "test-user",
        uniqueUserId = null,
        email = email,
        phone = null,
        firstName = "Tester",
        profileId = "test-profile",
        isGuest = false,
    )
}
