package com.vplayed.testing

import com.vplayed.shared.auth.AuthRepository
import com.vplayed.shared.auth.AuthSession
import com.vplayed.shared.auth.DeviceSignOutKey
import com.vplayed.shared.auth.EmailLoginResult
import com.vplayed.shared.auth.GuestLoginResult
import com.vplayed.shared.auth.LogoutResult
import com.vplayed.shared.auth.OtpVerificationResult
import com.vplayed.shared.auth.SignOutDeviceResult
import kotlinx.coroutines.CompletableDeferred

/** A signed-in session as the shared layer returns it (synthetic). */
val testAuthSession = AuthSession(
    accessToken = "access-1",
    refreshToken = "refresh-1",
    userId = "u-1",
    uniqueUserId = 42,
    email = "user@example.com",
    phone = null,
    firstName = "Sam",
    profileId = "p-1",
    isGuest = false,
)

fun verified(isNewUser: Boolean = false, hasMultipleProfiles: Boolean = false) =
    OtpVerificationResult.Verified(testAuthSession, isNewUser, hasMultipleProfiles)

/** Scriptable shared [AuthRepository]: records each call and answers with the configured result, optionally held. */
class FakeAuthRepository : AuthRepository {
    var result: EmailLoginResult = EmailLoginResult.CodeSent(message = null, isNewUser = false)
    var verifyResult: OtpVerificationResult = verified()
    var continueResult: OtpVerificationResult = verified()
    var signOutResult: SignOutDeviceResult = SignOutDeviceResult.Success
    var logoutResult: LogoutResult = LogoutResult.LoggedOut

    /** Holds [requestEmailLogin] until completed. */
    var hold: CompletableDeferred<Unit>? = null
    var verifyHold: CompletableDeferred<Unit>? = null
    var signOutHold: CompletableDeferred<Unit>? = null
    var logoutHold: CompletableDeferred<Unit>? = null
    var logouts = 0

    val requestedEmails = mutableListOf<String>()
    val verifications = mutableListOf<Pair<String, String>>()
    val signOuts = mutableListOf<Pair<DeviceSignOutKey, Boolean>>()
    val continuedEmails = mutableListOf<String>()

    override suspend fun requestEmailLogin(email: String): EmailLoginResult {
        requestedEmails += email
        hold?.await()
        return result
    }

    override suspend fun verifyEmailOtp(email: String, code: String): OtpVerificationResult {
        verifications += email to code
        verifyHold?.await()
        return verifyResult
    }

    override suspend fun signOutDevice(key: DeviceSignOutKey, all: Boolean): SignOutDeviceResult {
        signOuts += key to all
        signOutHold?.await()
        return signOutResult
    }

    override suspend fun continueAfterDeviceSignOut(email: String): OtpVerificationResult {
        continuedEmails += email
        return continueResult
    }

    // This app has no guest login.
    override suspend fun guestLogin(): GuestLoginResult = error("not used by AndroidJP")

    override suspend fun logout(): LogoutResult {
        logouts++
        logoutHold?.await()
        return logoutResult
    }
}
