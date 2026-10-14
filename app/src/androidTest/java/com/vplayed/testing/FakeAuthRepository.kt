package com.vplayed.testing

import com.vplayed.core.session.SessionRepository
import com.vplayed.core.session.UserSession
import com.vplayed.feature.auth.domain.AuthRepository
import com.vplayed.feature.auth.domain.EmailLoginResult
import com.vplayed.feature.auth.domain.model.DeviceSignOutKey
import com.vplayed.feature.auth.domain.model.OtpVerificationResult
import com.vplayed.feature.auth.domain.model.SignOutDeviceResult
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

/** Login without a backend: records each call, answers with the configured results and stores a session on a successful verify. */
@Singleton
class FakeAuthRepository @Inject constructor(private val sessions: SessionRepository) : AuthRepository {
    @Volatile
    var result: EmailLoginResult = EmailLoginResult.CodeSent(message = "Code sent", isNewUser = false)

    @Volatile
    var verifyResult: OtpVerificationResult = OtpVerificationResult.Verified(isNewUser = false, hasMultipleProfiles = false)

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
        return verifyResult.also { if (it is OtpVerificationResult.Verified) sessions.save(testSession(email)) }
    }

    override suspend fun signOutDevice(key: DeviceSignOutKey, all: Boolean): SignOutDeviceResult = signOutResult

    override suspend fun continueAfterDeviceSignOut(email: String): OtpVerificationResult =
        verifyResult.also { if (it is OtpVerificationResult.Verified) sessions.save(testSession(email)) }

    private fun testSession(email: String) = UserSession("test-user", email, "Tester", "test-access-token", "test-refresh-token", "test-profile")
}
