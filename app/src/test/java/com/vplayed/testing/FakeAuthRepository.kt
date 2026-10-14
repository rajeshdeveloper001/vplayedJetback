package com.vplayed.testing

import com.vplayed.feature.auth.domain.AuthRepository
import com.vplayed.feature.auth.domain.EmailLoginResult
import com.vplayed.feature.auth.domain.model.DeviceSignOutKey
import com.vplayed.feature.auth.domain.model.OtpVerificationResult
import com.vplayed.feature.auth.domain.model.SignOutDeviceResult
import kotlinx.coroutines.CompletableDeferred

/** Scriptable [AuthRepository]: records each call and answers with the configured result, optionally held. */
class FakeAuthRepository : AuthRepository {
    var result: EmailLoginResult = EmailLoginResult.CodeSent(message = null, isNewUser = false)
    var verifyResult: OtpVerificationResult = OtpVerificationResult.Verified(isNewUser = false, hasMultipleProfiles = false)
    var continueResult: OtpVerificationResult = OtpVerificationResult.Verified(isNewUser = false, hasMultipleProfiles = false)
    var signOutResult: SignOutDeviceResult = SignOutDeviceResult.Success

    /** Holds [requestEmailLogin] until completed. */
    var hold: CompletableDeferred<Unit>? = null
    var verifyHold: CompletableDeferred<Unit>? = null
    var signOutHold: CompletableDeferred<Unit>? = null

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
}
