package com.vplayed.feature.auth.data

import com.vplayed.core.dispatcher.IoDispatcher
import com.vplayed.core.error.AppError
import com.vplayed.core.session.SessionRepository
import com.vplayed.core.session.UserSession
import com.vplayed.feature.auth.domain.AuthRepository
import com.vplayed.feature.auth.domain.EmailLoginResult
import com.vplayed.feature.auth.domain.model.DeviceKind
import com.vplayed.feature.auth.domain.model.DeviceLimit
import com.vplayed.feature.auth.domain.model.DeviceSignOutKey
import com.vplayed.feature.auth.domain.model.OtpVerificationResult
import com.vplayed.feature.auth.domain.model.SignOutDeviceResult
import com.vplayed.feature.auth.domain.model.SignedInDevice
import com.vplayed.shared.SharedGraph
import com.vplayed.shared.auth.AuthSession
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import com.vplayed.shared.auth.DeviceKind as SharedDeviceKind
import com.vplayed.shared.auth.DeviceLimit as SharedDeviceLimit
import com.vplayed.shared.auth.DeviceSignOutKey as SharedDeviceSignOutKey
import com.vplayed.shared.auth.EmailLoginResult as SharedEmailLoginResult
import com.vplayed.shared.auth.OtpVerificationResult as SharedOtpVerificationResult
import com.vplayed.shared.auth.SignOutDeviceResult as SharedSignOutDeviceResult
import com.vplayed.shared.core.error.AppError as SharedAppError

/**
 * Email login through the KMP shared layer (`SharedGraph.authRepository`, ../kmp), which holds the
 * request encryption, the endpoints and the status-code rules both apps follow (decisions D1–D5 in
 * `KMP_PLANNING/KMP_ARCHITECTURE_ANALYSIS.md`). This class only maps its results to the app's
 * domain types and stores a verified session. [DefaultAuthRepository] (Retrofit) stays until the
 * Phase 7 cleanup.
 */
@Singleton
class SharedAuthRepository @Inject constructor(
    private val graph: SharedGraph,
    private val sessions: SessionRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : AuthRepository {

    private val shared get() = graph.authRepository

    // Encryption runs before the request, so stay off the main thread.
    override suspend fun requestEmailLogin(email: String): EmailLoginResult = withContext(ioDispatcher) {
        when (val result = shared.requestEmailLogin(email)) {
            is SharedEmailLoginResult.CodeSent -> EmailLoginResult.CodeSent(result.message, result.isNewUser)
            is SharedEmailLoginResult.DeviceLimitReached -> EmailLoginResult.DeviceLimitReached(result.message, result.limit?.toDomain())
            is SharedEmailLoginResult.Rejected -> EmailLoginResult.Rejected(result.message)
            is SharedEmailLoginResult.Failure -> EmailLoginResult.Failure(result.error.toDomain())
        }
    }

    override suspend fun verifyEmailOtp(email: String, code: String): OtpVerificationResult = withContext(ioDispatcher) {
        shared.verifyEmailOtp(email, code).toDomain(email.trim())
    }

    override suspend fun signOutDevice(key: DeviceSignOutKey, all: Boolean): SignOutDeviceResult = withContext(ioDispatcher) {
        when (val result = shared.signOutDevice(SharedDeviceSignOutKey(key.username, key.accessToken), all)) {
            SharedSignOutDeviceResult.Success -> SignOutDeviceResult.Success
            is SharedSignOutDeviceResult.Rejected -> SignOutDeviceResult.Rejected(result.message)
            is SharedSignOutDeviceResult.Failure -> SignOutDeviceResult.Failure(result.error.toDomain())
        }
    }

    override suspend fun continueAfterDeviceSignOut(email: String): OtpVerificationResult = withContext(ioDispatcher) {
        shared.continueAfterDeviceSignOut(email).toDomain(email.trim())
    }

    /** Only a verified login writes the session, before [OtpVerificationResult.Verified] is returned. */
    private suspend fun SharedOtpVerificationResult.toDomain(requestedEmail: String): OtpVerificationResult = when (this) {
        is SharedOtpVerificationResult.Verified -> {
            sessions.save(session.toUserSession(requestedEmail))
            OtpVerificationResult.Verified(isNewUser, hasMultipleProfiles)
        }
        is SharedOtpVerificationResult.RegistrationRequired -> OtpVerificationResult.RegistrationRequired(message)
        is SharedOtpVerificationResult.DeviceLimitReached -> OtpVerificationResult.DeviceLimitReached(message, limit?.toDomain())
        is SharedOtpVerificationResult.Rejected -> OtpVerificationResult.Rejected(message)
        is SharedOtpVerificationResult.Failure -> OtpVerificationResult.Failure(error.toDomain())
    }

    private fun AuthSession.toUserSession(requestedEmail: String): UserSession {
        val userEmail = email ?: requestedEmail
        return UserSession(
            userId = userId ?: userEmail,
            email = userEmail,
            displayName = firstName,
            accessToken = accessToken,
            refreshToken = refreshToken,
            profileId = profileId,
        )
    }

    private fun SharedDeviceLimit.toDomain() = DeviceLimit(
        devices = devices.map { device ->
            SignedInDevice(
                name = device.name,
                kind = device.kind.toDomain(),
                detail = device.detail,
                isCurrent = device.isCurrent,
                signOutKey = DeviceSignOutKey(device.signOutKey.username, device.signOutKey.accessToken),
            )
        },
        maxDevices = maxDevices,
    )

    private fun SharedDeviceKind.toDomain(): DeviceKind = when (this) {
        SharedDeviceKind.DESKTOP -> DeviceKind.DESKTOP
        SharedDeviceKind.PHONE -> DeviceKind.PHONE
        SharedDeviceKind.TV -> DeviceKind.TV
        SharedDeviceKind.OTHER -> DeviceKind.OTHER
    }

    private fun SharedAppError.toDomain(): AppError = when (this) {
        // The app explains timeouts with its connection message.
        SharedAppError.Network, SharedAppError.Timeout -> AppError.Network
        SharedAppError.Unauthorized -> AppError.Unauthorized
        is SharedAppError.Server -> AppError.Server(code, message)
        SharedAppError.Parsing -> AppError.Parsing
        is SharedAppError.Configuration -> AppError.Unknown(IllegalStateException(reason))
        is SharedAppError.Unknown -> AppError.Unknown(cause)
    }
}
