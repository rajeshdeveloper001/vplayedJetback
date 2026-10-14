package com.vplayed.core.network

import com.vplayed.core.common.config.AppConfig
import com.vplayed.core.session.SessionRepository
import com.vplayed.shared.geo.GeoStore
import java.util.Locale
import javax.inject.Inject
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Headers the VPlayed backend expects from the mobile app (same names as the previous app).
 *
 * - `device_id` is sent when the platform provides one.
 * - `x-bearer-token` carries the latest geo token from [GeoStore] when one is known;
 *   it is omitted (never empty) otherwise, and on requests marked [WithoutGeoToken].
 * - With a signed-in session: `Authorization: Bearer <access token>` and `profile_id` (omitted
 *   when the user has no default profile id). Nothing is sent for a signed-out user.
 */
class ApiHeadersInterceptor @Inject constructor(
    private val config: AppConfig,
    private val geoStore: GeoStore,
    private val deviceIdProvider: DeviceIdProvider,
    private val sessionRepository: SessionRepository,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val builder = original.newBuilder()
            .header("Accept", "application/json")
            .header("X-REQUEST-TYPE", "mobile")
            .header("app_version", config.versionName)
            .header("device_type", "android")
            .header("X-LANGUAGE-CODE", Locale.getDefault().language.ifBlank { DEFAULT_LANGUAGE })
        deviceIdProvider.deviceId?.let { builder.header(DEVICE_ID_HEADER, it) }
        sessionRepository.session.value?.let { session ->
            if (session.accessToken.isNotBlank()) {
                builder.header(AUTHORIZATION_HEADER, "Bearer ${session.accessToken}")
                session.profileId?.takeIf { it.isNotBlank() }?.let { builder.header(PROFILE_ID_HEADER, it) }
            }
        }
        val geoToken = geoStore.geoToken
        if (!geoToken.isNullOrBlank() && !original.hasMethodAnnotation<WithoutGeoToken>()) {
            builder.header(GEO_TOKEN_HEADER, geoToken)
        }
        return chain.proceed(builder.build())
    }

    companion object {
        const val GEO_TOKEN_HEADER = "x-bearer-token"
        const val DEVICE_ID_HEADER = "device_id"
        const val AUTHORIZATION_HEADER = "Authorization"
        const val PROFILE_ID_HEADER = "profile_id"
        private const val DEFAULT_LANGUAGE = "en"
    }
}
