package com.vplayed.core.shared

import com.vplayed.core.common.config.AppConfig
import com.vplayed.core.common.config.AppEnvironment
import com.vplayed.core.geo.GeoRestrictionStore
import com.vplayed.core.network.ApiHeadersInterceptor
import com.vplayed.core.network.DeviceIdProvider
import com.vplayed.core.network.DeviceNameProvider
import com.vplayed.core.session.UserSession
import com.vplayed.shared.PlatformDependencies
import com.vplayed.shared.SharedGraph
import com.vplayed.shared.core.crypto.platformCryptoPrimitives
import com.vplayed.shared.core.network.AuthTokens
import com.vplayed.shared.core.network.RequestOptions
import com.vplayed.shared.core.network.createOkHttpEngine
import com.vplayed.shared.core.platform.NoOpLogger
import com.vplayed.testing.FakeSessionRepository
import kotlinx.coroutines.test.runTest
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Until features move to the shared layer, both stacks must send the backend identical headers.
 * The same request goes through `ApiHeadersInterceptor` (Retrofit/OkHttp path) and through the
 * shared Ktor client on the OkHttp engine; a terminal interceptor captures both without network.
 */
class SharedHeadersParityTest {

    private val appConfig = AppConfig(AppEnvironment.DEVELOPMENT, "https://api.test/", "https://images.test/", true, true, "2.4")
    private val geoStore = GeoRestrictionStore()
    private val sessions = FakeSessionRepository()
    private val deviceIdProvider = object : DeviceIdProvider {
        override val deviceId: String? = "device-123"
    }
    private val deviceNameProvider = object : DeviceNameProvider {
        override val deviceName: String = "Pixel 9"
    }

    private val captured = mutableListOf<Request>()
    private val capture = Interceptor { chain ->
        captured += chain.request()
        Response.Builder()
            .request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body("""{"statusCode":200}""".toResponseBody("application/json".toMediaType()))
            .build()
    }

    private val legacyClient = OkHttpClient.Builder()
        .addInterceptor(ApiHeadersInterceptor(appConfig, geoStore, deviceIdProvider, sessions))
        .addInterceptor(capture)
        .build()

    private val graph = SharedGraph(
        appConfig.toSharedConfig(),
        PlatformDependencies(
            deviceInfo = AndroidSharedDeviceInfo(deviceIdProvider, deviceNameProvider),
            logger = NoOpLogger,
            engineFactory = { createOkHttpEngine(OkHttpClient.Builder().addInterceptor(capture).build()) },
            cryptoPrimitives = platformCryptoPrimitives(),
        ),
    )

    private suspend fun headersOfBoth(sharedOptions: RequestOptions = RequestOptions.Default): Pair<Headers, Headers> {
        captured.clear()
        legacyClient.newCall(Request.Builder().url("https://api.test/mediaview/api/v1/home").build()).execute().close()
        graph.apiCaller.get("mediaview/api/v1/home", options = sharedOptions)
        check(captured.size == 2) { "expected one request per stack, got ${captured.size}" }
        return captured[0].headers to captured[1].headers
    }

    private fun assertSameVPlayedHeaders(legacy: Headers, shared: Headers) {
        VPLAYED_HEADERS.forEach { name -> assertEquals(name, legacy[name], shared[name]) }
    }

    @Test
    fun givenSignedOut_whenSendingTheSameRequest_thenBothStacksSendTheSameHeaders() = runTest {
        val (legacy, shared) = headersOfBoth()

        assertSameVPlayedHeaders(legacy, shared)
        assertEquals("device-123", shared["device_id"])
        assertNull(shared["Authorization"])
    }

    @Test
    fun givenSessionAndGeoToken_whenSendingTheSameRequest_thenBothStacksSendTheSameHeaders() = runTest {
        val session = UserSession(userId = "u1", email = "user@example.com", accessToken = "access-abc", profileId = "profile-1")
        sessions.save(session)
        geoStore.updateGeo("geo-xyz", null)
        // Phase 3/5 moves session and geo state into the shared layer; until then the app mirrors them.
        graph.requestCredentials.setAuthTokens(AuthTokens(session.accessToken, session.profileId))
        graph.requestCredentials.setGeoToken("geo-xyz")

        val (legacy, shared) = headersOfBoth()

        assertSameVPlayedHeaders(legacy, shared)
        assertEquals("Bearer access-abc", shared["Authorization"])
        assertEquals("profile-1", shared["profile_id"])
        assertEquals("geo-xyz", shared["x-bearer-token"])
    }

    @Test
    fun givenRequestOptingOutOfGeoToken_whenSent_thenSharedStackOmitsItLikeWithoutGeoToken() = runTest {
        geoStore.updateGeo("geo-xyz", null)
        graph.requestCredentials.setGeoToken("geo-xyz")

        val (_, shared) = headersOfBoth(RequestOptions(sendGeoToken = false))

        assertNull(shared["x-bearer-token"])
    }

    private companion object {
        /** Headers the backend reads (names are case-insensitive in OkHttp's Headers). */
        val VPLAYED_HEADERS = listOf(
            "Accept", "X-Request-Type", "device_type", "app_version", "X-Language-Code",
            "device_id", "Authorization", "profile_id", "x-bearer-token",
        )
    }
}
