package com.vplayed.core.network

import com.vplayed.core.common.config.AppConfig
import com.vplayed.core.common.config.AppEnvironment
import com.vplayed.shared.geo.GeoInfo
import com.vplayed.shared.geo.GeoStore
import com.vplayed.testing.testAuthSession
import com.vplayed.testing.testSessionManager
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.create
import retrofit2.http.GET

class ApiHeadersInterceptorTest {

    private interface TestApi {
        @GET("plain")
        fun plain(): Call<ResponseBody>

        @WithoutGeoToken
        @GET("geo")
        fun withoutGeoToken(): Call<ResponseBody>
    }

    private val server = MockWebServer()
    private val config = AppConfig(AppEnvironment.DEVELOPMENT, "https://api.test/", "https://images.test/", true, true, "2.4")
    private val store = GeoStore()
    private var deviceId: String? = "device-123"
    private val sessions = testSessionManager()

    private val client = OkHttpClient.Builder()
        .addInterceptor(
            ApiHeadersInterceptor(
                config,
                store,
                object : DeviceIdProvider {
                    override val deviceId: String? get() = this@ApiHeadersInterceptorTest.deviceId
                },
                sessions,
            ),
        )
        .build()

    private lateinit var api: TestApi

    @Before
    fun setUp() {
        server.start()
        api = Retrofit.Builder().baseUrl(server.url("/")).client(client).build().create<TestApi>()
    }

    @After
    fun tearDown() = server.close()

    private fun sendPlain(): okhttp3.Headers {
        server.enqueue(MockResponse.Builder().body("{}").build())
        api.plain().execute().body()?.close()
        return server.takeRequest().headers
    }

    @Test
    fun givenAnyRequest_whenSent_thenCarriesMobileAppHeaders() {
        server.enqueue(MockResponse.Builder().body("{}").build())

        client.newCall(Request.Builder().url(server.url("/ping")).build()).execute().close()

        val headers = server.takeRequest().headers
        assertEquals("application/json", headers["Accept"])
        assertEquals("mobile", headers["X-REQUEST-TYPE"])
        assertEquals("2.4", headers["app_version"])
        assertEquals("android", headers["device_type"])
        assertEquals(true, headers["X-LANGUAGE-CODE"]?.isNotBlank())
        assertEquals("device-123", headers["device_id"])
    }

    @Test
    fun givenNoDeviceId_whenSent_thenDeviceIdHeaderOmitted() {
        deviceId = null
        assertNull(sendPlain()["device_id"])
    }

    @Test
    fun givenNoGeoToken_whenSent_thenGeoHeaderOmittedInsteadOfNullOrEmpty() {
        assertNull(sendPlain()["x-bearer-token"])
    }

    @Test
    fun givenBlankGeoToken_whenStored_thenGeoHeaderStillOmitted() {
        store.updateGeo("   ", GeoInfo(currency = "USD"))
        assertNull(sendPlain()["x-bearer-token"])
    }

    @Test
    fun givenGeoToken_whenSent_thenCarriesLatestToken() {
        store.updateGeo("token-1", null)
        assertEquals("token-1", sendPlain()["x-bearer-token"])

        store.updateGeo("token-2", null)
        assertEquals("token-2", sendPlain()["x-bearer-token"])
    }

    @Test
    fun givenGeoToken_whenMethodMarkedWithoutGeoToken_thenHeaderOmitted() {
        store.updateGeo("token-1", null)
        server.enqueue(MockResponse.Builder().body("{}").build())

        api.withoutGeoToken().execute().body()?.close()

        assertNull(server.takeRequest().headers["x-bearer-token"])
    }

    @Test
    fun givenSignedOut_whenSent_thenNoAuthorizationOrProfileHeaders() {
        val headers = sendPlain()
        assertNull(headers["Authorization"])
        assertNull(headers["profile_id"])
    }

    @Test
    fun givenSession_whenSent_thenCarriesBearerTokenAndProfileId() = kotlinx.coroutines.test.runTest {
        sessions.signIn(testAuthSession.copy(accessToken = "access-1", profileId = "profile-1"))

        val headers = sendPlain()

        assertEquals("Bearer access-1", headers["Authorization"])
        assertEquals("profile-1", headers["profile_id"])
    }

    @Test
    fun givenSessionWithoutProfile_whenSent_thenProfileHeaderOmittedNotEmpty() = kotlinx.coroutines.test.runTest {
        sessions.signIn(testAuthSession.copy(accessToken = "access-1", profileId = null))

        val headers = sendPlain()

        assertEquals("Bearer access-1", headers["Authorization"])
        assertNull(headers["profile_id"])
    }

    @Test
    fun givenSessionWithBlankToken_whenSent_thenNoAuthorizationHeader() = kotlinx.coroutines.test.runTest {
        sessions.signIn(testAuthSession.copy(accessToken = "  ", profileId = "profile-1"))

        val headers = sendPlain()

        assertNull(headers["Authorization"])
        assertNull(headers["profile_id"])
    }
}
