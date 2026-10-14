package com.vplayed.core.shared

import com.vplayed.core.common.config.AppConfig
import com.vplayed.core.common.config.AppEnvironment
import com.vplayed.core.network.DeviceIdProvider
import com.vplayed.core.network.DeviceNameProvider
import com.vplayed.shared.PlatformDependencies
import com.vplayed.shared.SharedGraph
import com.vplayed.shared.core.crypto.platformCryptoPrimitives
import com.vplayed.shared.core.network.RequestOptions
import com.vplayed.shared.core.network.createOkHttpEngine
import com.vplayed.shared.core.platform.NoOpLogger
import com.vplayed.shared.geo.GeoStore
import com.vplayed.testing.testAuthSession
import com.vplayed.testing.testSessionManager
import java.util.Locale
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
 * The headers this app's backend requests carry, built the way the app builds them: `AppConfig`
 * mapped by `toSharedConfig`, `AndroidSharedDeviceInfo`, the shared `SessionManager` and `GeoStore`,
 * on the OkHttp engine. A terminal interceptor captures the request without network.
 */
class SharedHeadersTest {

    private val appConfig = AppConfig(AppEnvironment.DEVELOPMENT, "https://api.test/", "https://images.test/", true, true, "2.4")
    private val geoStore = GeoStore()
    private val sessions = testSessionManager()
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

    private val graph = SharedGraph(
        appConfig.toSharedConfig(),
        PlatformDependencies(
            deviceInfo = AndroidSharedDeviceInfo(deviceIdProvider, deviceNameProvider),
            logger = NoOpLogger,
            engineFactory = { createOkHttpEngine(OkHttpClient.Builder().addInterceptor(capture).build()) },
            cryptoPrimitives = platformCryptoPrimitives(),
        ),
        sessions,
        geoStore,
    )

    private suspend fun headersOf(options: RequestOptions = RequestOptions.Default): Headers {
        captured.clear()
        graph.apiCaller.get("mediaview/api/v1/home", options = options)
        return captured.single().headers
    }

    @Test
    fun givenSignedOut_whenRequesting_thenDeviceHeadersOnly() = runTest {
        val headers = headersOf()

        assertEquals("application/json", headers["Accept"])
        assertEquals("mobile", headers["X-Request-Type"])
        assertEquals("android", headers["device_type"])
        assertEquals("2.4", headers["app_version"])
        assertEquals(Locale.getDefault().language.ifBlank { "en" }, headers["X-Language-Code"])
        assertEquals("device-123", headers["device_id"])
        assertNull(headers["Authorization"])
        assertNull(headers["profile_id"])
        assertNull(headers["x-bearer-token"])
    }

    @Test
    fun givenSessionAndGeoToken_whenRequesting_thenBothAreSent() = runTest {
        sessions.signIn(testAuthSession.copy(accessToken = "access-abc", profileId = "profile-1"))
        geoStore.updateGeo("geo-xyz", null)

        val headers = headersOf()

        assertEquals("Bearer access-abc", headers["Authorization"])
        assertEquals("profile-1", headers["profile_id"])
        assertEquals("geo-xyz", headers["x-bearer-token"])
    }

    @Test
    fun givenRequestOptingOutOfGeoToken_whenSent_thenTheGeoTokenIsLeftOut() = runTest {
        geoStore.updateGeo("geo-xyz", null)

        val headers = headersOf(RequestOptions(sendGeoToken = false))

        assertNull(headers["x-bearer-token"])
    }
}
