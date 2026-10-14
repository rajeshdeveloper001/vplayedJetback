package com.vplayed.feature.auth.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

/** Request wrapper: `{"data": "<encrypted JSON>"}` (previous app's `DataProperties`). */
@Serializable
data class DataPropertiesDto(@SerialName("data") val data: String)

/** Plain JSON that is encrypted into [DataPropertiesDto.data] (previous app's `EmailLoginProperties`). */
@Serializable
data class EmailLoginPropertiesDto(@SerialName("email") val email: String)

/** Plain JSON encrypted for `verifyEmailOtp` (previous app's `EmailVerifyProperties`). */
@Serializable
data class EmailVerifyPropertiesDto(
    @SerialName("email") val email: String,
    @SerialName("otp") val otp: String,
    @SerialName("deviceDetail") val deviceDetail: String,
)

/**
 * Envelope of the previous app's `UserResponse` (`ResponseStatus` + `response`). [response] is kept
 * as raw JSON because its type varies (a user object on success, anything on errors); it is
 * decoded into [UserDto] only where a user is expected.
 */
@Serializable
data class UserResponseDto(
    @SerialName("error") val error: Boolean? = null,
    @SerialName("statusCode") val statusCode: Int? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("message") val message: String? = null,
    @SerialName("response") val response: JsonElement? = null,
)

/** The fields of the previous app's `User` that the login flow needs. Tokens are credentials. */
@Serializable
data class UserDto(
    @SerialName("_id") val id: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("first_name") val firstName: String? = null,
    @SerialName("access_token") val accessToken: String? = null,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("default_profile") val defaultProfile: DefaultProfileDto? = null,
    // Numbers here arrive as numbers or strings; read the content leniently.
    @SerialName("profile_count") val profileCount: JsonPrimitive? = null,
    @SerialName("isNewUsers") val isNewUsers: Boolean? = null,
    @SerialName("get_devices") val devices: List<DeviceDetailDto>? = null,
    @SerialName("device_limit") val deviceLimit: JsonPrimitive? = null,
)

@Serializable
data class DefaultProfileDto(@SerialName("_id") val id: String? = null)

/** One signed-in device (previous app's `DeviceDetail`). */
@Serializable
data class DeviceDetailDto(
    @SerialName("username") val username: String? = null,
    @SerialName("access_token") val accessToken: String? = null,
    @SerialName("device_name") val deviceName: String? = null,
    @SerialName("device_type") val deviceType: String? = null,
    @SerialName("browser_name") val browserName: String? = null,
    @SerialName("current_device") val currentDevice: JsonPrimitive? = null,
)
