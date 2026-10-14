package com.vplayed.feature.auth.data

import com.vplayed.core.common.config.AppConfig
import com.vplayed.core.dispatcher.IoDispatcher
import com.vplayed.core.error.AppError
import com.vplayed.core.error.DataResult
import com.vplayed.core.error.resultOf
import com.vplayed.core.network.DeviceNameProvider
import com.vplayed.core.security.CryptoTransport
import com.vplayed.core.session.SessionRepository
import com.vplayed.core.session.UserSession
import com.vplayed.feature.auth.data.remote.AuthApi
import com.vplayed.feature.auth.data.remote.dto.DataPropertiesDto
import com.vplayed.feature.auth.data.remote.dto.DeviceDetailDto
import com.vplayed.feature.auth.data.remote.dto.EmailLoginPropertiesDto
import com.vplayed.feature.auth.data.remote.dto.EmailVerifyPropertiesDto
import com.vplayed.feature.auth.data.remote.dto.UserDto
import com.vplayed.feature.auth.data.remote.dto.UserResponseDto
import com.vplayed.feature.auth.domain.AuthRepository
import com.vplayed.feature.auth.domain.EmailLoginResult
import com.vplayed.feature.auth.domain.model.DeviceKind
import com.vplayed.feature.auth.domain.model.DeviceLimit
import com.vplayed.feature.auth.domain.model.DeviceSignOutKey
import com.vplayed.feature.auth.domain.model.OtpVerificationResult
import com.vplayed.feature.auth.domain.model.SignOutDeviceResult
import com.vplayed.feature.auth.domain.model.SignedInDevice
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import retrofit2.Response

/**
 * Email login against the previous app's endpoints (`docs/api/auth-api.md`): requests with an
 * encrypted `{"data": …}` body, answers interpreted by HTTP status first (200 / 202 / 203) and then
 * by the body's `error` flag, never by "HTTP 200 means success". Only a verified login writes the
 * session. Nothing about the email, code, tokens or ciphertext is logged.
 */
@Singleton
class DefaultAuthRepository @Inject constructor(
    private val api: AuthApi,
    private val crypto: CryptoTransport,
    private val config: AppConfig,
    private val json: Json,
    private val deviceName: DeviceNameProvider,
    private val sessions: SessionRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : AuthRepository {

    override suspend fun requestEmailLogin(email: String): EmailLoginResult = withContext(ioDispatcher) {
        val payload = json.encodeToString(EmailLoginPropertiesDto(email.trim()))
        val cipherText = encrypt(payload)
            ?: return@withContext EmailLoginResult.Failure(AppError.Unknown(MissingTransportKeyException()))
        when (val result = resultOf { api.loginWithEmail(DataPropertiesDto(cipherText)) }) {
            is DataResult.Failure -> EmailLoginResult.Failure(result.error)
            is DataResult.Success -> result.data.toLoginResult()
        }
    }

    override suspend fun verifyEmailOtp(email: String, code: String): OtpVerificationResult = withContext(ioDispatcher) {
        val payload = json.encodeToString(EmailVerifyPropertiesDto(email.trim(), code, deviceName.deviceName))
        val cipherText = encrypt(payload)
            ?: return@withContext OtpVerificationResult.Failure(AppError.Unknown(MissingTransportKeyException()))
        when (val result = resultOf { api.verifyEmailOtp(DataPropertiesDto(cipherText)) }) {
            is DataResult.Failure -> OtpVerificationResult.Failure(result.error)
            is DataResult.Success -> result.data.toVerification(email.trim())
        }
    }

    override suspend fun continueAfterDeviceSignOut(email: String): OtpVerificationResult = withContext(ioDispatcher) {
        val result = resultOf {
            api.verifyEmailLogin(
                emailVerified = "1",
                email = email.trim(),
                deviceDetail = deviceName.deviceName,
                browserName = "",
            )
        }
        when (result) {
            is DataResult.Failure -> OtpVerificationResult.Failure(result.error)
            is DataResult.Success -> result.data.toVerification(email.trim())
        }
    }

    override suspend fun signOutDevice(key: DeviceSignOutKey, all: Boolean): SignOutDeviceResult = withContext(ioDispatcher) {
        val result = resultOf {
            api.logoutDevice(key.username, key.accessToken, if (all) PREFERENCE_ALL else PREFERENCE_ONE)
        }
        when (result) {
            is DataResult.Failure -> SignOutDeviceResult.Failure(result.error)
            is DataResult.Success -> result.data.toSignOutResult()
        }
    }

    private fun encrypt(plain: String): String? = crypto.encryptForTransport(config.transportKey, plain)

    // region email-login

    private fun Response<UserResponseDto>.toLoginResult(): EmailLoginResult {
        val code = code()
        if (isSuccessful) {
            val body = body() ?: return EmailLoginResult.Failure(AppError.Parsing)
            return when {
                // 202 and 203 are decided by the status alone, as in the previous app (it never reads the
                // body's `error` flag for them).
                code == HTTP_DEVICE_LIMIT ->
                    EmailLoginResult.DeviceLimitReached(body.message.orNullIfBlank(), body.user()?.toDeviceLimit())
                code == HTTP_NEW_USER -> EmailLoginResult.CodeSent(body.message.orNullIfBlank(), isNewUser = true)
                // A 200 that reports an API error is not "code sent".
                body.error == true -> EmailLoginResult.Rejected(body.message.orNullIfBlank())
                code == HTTP_OK -> EmailLoginResult.CodeSent(body.message.orNullIfBlank(), isNewUser = false)
                // Any other 2xx is not part of the contract: do not report it as a success.
                else -> EmailLoginResult.Failure(AppError.Server(code, body.message))
            }
        }
        val message = readErrorMessage()
        return when {
            message != null -> EmailLoginResult.Rejected(message)
            code == HTTP_UNAUTHORIZED -> EmailLoginResult.Failure(AppError.Unauthorized)
            else -> EmailLoginResult.Failure(AppError.Server(code))
        }
    }

    // endregion

    // region verify

    /** Shared by `verifyEmailOtp` and `verifyEmailLogin`: same response contract. */
    private suspend fun Response<UserResponseDto>.toVerification(email: String): OtpVerificationResult {
        val code = code()
        if (isSuccessful) {
            val body = body() ?: return OtpVerificationResult.Failure(AppError.Parsing)
            val message = body.message.orNullIfBlank()
            val user = body.user()
            return when {
                code == HTTP_DEVICE_LIMIT -> OtpVerificationResult.DeviceLimitReached(message, user?.toDeviceLimit())
                // A 200 that reports an API error (wrong or expired code) is a refusal, not a login.
                // 203 is decided by the status, as in the previous app.
                body.error == true && code != HTTP_NEW_USER -> OtpVerificationResult.Rejected(message)
                code == HTTP_OK || code == HTTP_NEW_USER -> {
                    val session = user?.toSession(email)
                    when {
                        session != null -> {
                            sessions.save(session)
                            OtpVerificationResult.Verified(
                                isNewUser = code == HTTP_NEW_USER || user.isNewUsers == true || user.firstName.isNullOrBlank(),
                                hasMultipleProfiles = (user.profileCount?.content?.toIntOrNull() ?: 0) > 1,
                            )
                        }
                        // A 203 without a usable session is the "register first" answer.
                        code == HTTP_NEW_USER -> OtpVerificationResult.RegistrationRequired(message)
                        // A 200 must carry a session; anything else is an unreadable answer, not a login.
                        else -> OtpVerificationResult.Failure(AppError.Parsing)
                    }
                }
                else -> OtpVerificationResult.Failure(AppError.Server(code, message))
            }
        }
        val message = readErrorMessage()
        return when {
            message != null -> OtpVerificationResult.Rejected(message)
            code == HTTP_UNAUTHORIZED -> OtpVerificationResult.Failure(AppError.Unauthorized)
            else -> OtpVerificationResult.Failure(AppError.Server(code))
        }
    }

    private fun UserDto.toSession(requestedEmail: String): UserSession? {
        val token = accessToken?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val userEmail = email?.trim()?.takeIf { it.isNotEmpty() } ?: requestedEmail
        return UserSession(
            userId = id?.takeIf { it.isNotBlank() } ?: userEmail,
            email = userEmail,
            displayName = firstName?.trim()?.takeIf { it.isNotEmpty() },
            accessToken = token,
            refreshToken = refreshToken?.takeIf { it.isNotBlank() },
            profileId = defaultProfile?.id?.takeIf { it.isNotBlank() },
        )
    }

    // endregion

    // region devices

    private fun Response<UserResponseDto>.toSignOutResult(): SignOutDeviceResult {
        if (!isSuccessful) {
            return readErrorMessage()?.let(SignOutDeviceResult::Rejected)
                ?: SignOutDeviceResult.Failure(AppError.Server(code()))
        }
        val body = body() ?: return SignOutDeviceResult.Failure(AppError.Parsing)
        // The previous app accepts only an explicit `error: false`.
        return if (body.error == false) {
            SignOutDeviceResult.Success
        } else {
            SignOutDeviceResult.Rejected(body.message.orNullIfBlank())
        }
    }

    private fun UserDto.toDeviceLimit(): DeviceLimit? {
        val list = devices.orEmpty().mapNotNull(::toSignedInDevice)
        if (list.isEmpty()) return null
        return DeviceLimit(devices = list, maxDevices = deviceLimit?.content?.toIntOrNull()?.takeIf { it > 0 })
    }

    private fun toSignedInDevice(dto: DeviceDetailDto): SignedInDevice? {
        val username = dto.username?.takeIf { it.isNotBlank() } ?: return null
        return SignedInDevice(
            name = dto.deviceName?.trim()?.takeIf { it.isNotEmpty() } ?: dto.deviceType?.trim().orEmpty(),
            kind = deviceKindOf(dto.deviceType),
            detail = dto.browserName?.trim()?.takeIf { it.isNotEmpty() },
            isCurrent = dto.currentDevice?.content.let { it == "1" || it.equals("true", ignoreCase = true) },
            signOutKey = DeviceSignOutKey(username, dto.accessToken),
        )
    }

    // endregion

    private fun UserResponseDto.user(): UserDto? {
        val element = response as? JsonObject ?: return null
        return try {
            json.decodeFromJsonElement(UserDto.serializer(), element)
        } catch (_: IllegalArgumentException) {
            // Unexpected shape (kotlinx SerializationException is an IllegalArgumentException).
            null
        }
    }

    /** Error responses use the same envelope; anything unreadable yields null. */
    private fun Response<*>.readErrorMessage(): String? {
        val raw = try {
            errorBody()?.use { it.string() }
        } catch (_: IOException) {
            null
        } ?: return null
        return try {
            json.decodeFromString<UserResponseDto>(raw).message.orNullIfBlank()
        } catch (_: IllegalArgumentException) {
            // Not JSON / not the envelope.
            null
        }
    }

    private fun String?.orNullIfBlank(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

    /** The build has no `SALT_KEY` (see app/build.gradle.kts), so no request can be encrypted. */
    class MissingTransportKeyException : IllegalStateException("Request encryption is not configured")

    internal companion object {
        const val HTTP_OK = 200
        const val HTTP_DEVICE_LIMIT = 202
        const val HTTP_NEW_USER = 203
        const val HTTP_UNAUTHORIZED = 401
        const val PREFERENCE_ONE = "1"
        const val PREFERENCE_ALL = "all"

        /** The backend's `device_type` values are not documented; match common names, else a generic icon. */
        fun deviceKindOf(type: String?): DeviceKind {
            val value = type?.lowercase().orEmpty()
            return when {
                "tv" in value -> DeviceKind.TV
                listOf("android", "ios", "iphone", "mobile", "phone", "tablet").any { it in value } -> DeviceKind.PHONE
                listOf("web", "browser", "desktop", "windows", "mac", "linux").any { it in value } -> DeviceKind.DESKTOP
                else -> DeviceKind.OTHER
            }
        }
    }
}
