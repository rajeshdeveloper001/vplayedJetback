package com.vplayed.feature.auth.domain.model

import com.vplayed.core.error.AppError

/** Kind of device shown in the device-limit list (icon only). */
enum class DeviceKind { DESKTOP, PHONE, TV, OTHER }

/** What is needed to sign one device out. Credentials of another device: never shown or logged. */
data class DeviceSignOutKey(val username: String, val accessToken: String?) {
    override fun toString(): String = "DeviceSignOutKey(***)"
}

data class SignedInDevice(
    val name: String,
    val kind: DeviceKind,
    /** Browser or app name, when the backend gives one. */
    val detail: String?,
    val isCurrent: Boolean,
    val signOutKey: DeviceSignOutKey,
)

/** The devices an account is signed in on when its limit is reached. */
data class DeviceLimit(val devices: List<SignedInDevice>, val maxDevices: Int?)

/** Outcome of verifying a login code (or of continuing after the device-limit step). */
sealed interface OtpVerificationResult {
    /** The user is signed in and the session is stored. */
    data class Verified(val isNewUser: Boolean, val hasMultipleProfiles: Boolean) : OtpVerificationResult

    /** HTTP 203 without a session: the account must be registered first (screen not built yet). */
    data class RegistrationRequired(val message: String?) : OtpVerificationResult

    /** HTTP 202: the account is signed in on the maximum number of devices. */
    data class DeviceLimitReached(val message: String?, val limit: DeviceLimit?) : OtpVerificationResult

    /** The backend refused the code (invalid, expired, …) and may say why in [message]. */
    data class Rejected(val message: String?) : OtpVerificationResult

    data class Failure(val error: AppError) : OtpVerificationResult
}

sealed interface SignOutDeviceResult {
    data object Success : SignOutDeviceResult
    data class Rejected(val message: String?) : SignOutDeviceResult
    data class Failure(val error: AppError) : SignOutDeviceResult
}
