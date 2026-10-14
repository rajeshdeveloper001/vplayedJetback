package com.vplayed.feature.auth.data.remote

import com.vplayed.core.network.RedactBody
import com.vplayed.feature.auth.data.remote.dto.DataPropertiesDto
import com.vplayed.feature.auth.data.remote.dto.UserResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

/**
 * Email login endpoints of the previous app (`GudShoServices`). The status code carries part of the
 * meaning (see `docs/api/auth-api.md`), so raw [Response]s are returned. Bodies hold one-time
 * codes, credentials and personal data, so every call is [RedactBody].
 */
interface AuthApi {
    /** Sends (and re-sends) the login code. The body is the encrypted [DataPropertiesDto]. */
    @RedactBody
    @POST("users/api/v1/auth/email-login")
    suspend fun loginWithEmail(@Body body: DataPropertiesDto): Response<UserResponseDto>

    /** Verifies the code. The body is the encrypted [DataPropertiesDto] of the email, code and device. */
    @RedactBody
    @POST("users/api/v1/auth/verifyEmailOtp")
    suspend fun verifyEmailOtp(@Body body: DataPropertiesDto): Response<UserResponseDto>

    /** Completes a login after the device-limit step, without a code (previous app's behaviour; form, not encrypted). */
    @RedactBody
    @FormUrlEncoded
    @POST("users/api/v1/auth/verifyEmailLogin")
    suspend fun verifyEmailLogin(
        @Field("is_email_verified") emailVerified: String,
        @Field("email") email: String,
        @Field("deviceDetail") deviceDetail: String,
        @Field("browsername") browserName: String,
    ): Response<UserResponseDto>

    /** Signs one device out (`preference = "1"`) or all of them (`"all"`) using that device's own credentials. */
    @RedactBody
    @FormUrlEncoded
    @POST("users/api/v1/auth/logout-device")
    suspend fun logoutDevice(
        @Field("username") username: String,
        @Field("access_token") accessToken: String?,
        @Field("preference") preference: String,
    ): Response<UserResponseDto>
}
