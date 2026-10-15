package com.vplayed.integration

import com.vplayed.core.common.config.AppConfig
import com.vplayed.core.common.config.AppEnvironment
import com.vplayed.core.geo.GeoLocation
import com.vplayed.core.geo.GeoRestrictionRepository
import com.vplayed.core.geo.data.GeoAccessKeyResponseDto
import com.vplayed.core.geo.data.GeoLocationDto
import com.vplayed.core.geo.data.GeoRestrictionApi
import com.vplayed.core.geo.data.PublicIpApi
import com.vplayed.core.geo.data.PublicIpDto
import com.vplayed.core.network.DeviceNameProvider
import com.vplayed.core.security.CryptoTransport
import com.vplayed.feature.auth.data.DefaultAuthRepository
import com.vplayed.feature.auth.data.remote.AuthApi
import com.vplayed.feature.auth.data.remote.dto.DataPropertiesDto
import com.vplayed.feature.auth.data.remote.dto.EmailLoginPropertiesDto
import com.vplayed.feature.auth.data.remote.dto.EmailVerifyPropertiesDto
import com.vplayed.feature.auth.domain.EmailLoginResult
import com.vplayed.feature.auth.domain.model.OtpVerificationResult
import com.vplayed.feature.dashboard.home.data.DefaultHomeRepository
import com.vplayed.feature.dashboard.home.data.mapper.HomeMapper
import com.vplayed.feature.dashboard.home.data.remote.HomeApi
import com.vplayed.feature.dashboard.home.domain.model.SectionStyle
import com.vplayed.testing.FakeSessionRepository
import com.vplayed.testing.Fixtures
import com.vplayed.testing.TransportCrypto
import java.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create

/**
 * Automated test suite implementing the complete API contract specifications defined in:
 * `docs/testing/specs/api-contract-and-integration-suite.yaml`.
 */
class ApiContractValidationTest {

    private val server = MockWebServer()
    private lateinit var retrofit: Retrofit

    @Before
    fun setUp() {
        server.start()
        retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(Fixtures.json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @After
    fun tearDown() = server.close()

    private fun enqueue(code: Int, body: String) =
        server.enqueue(MockResponse.Builder().code(code).body(body).build())

    // =========================================================================
    // 1. Public IP & Geo Restriction Contracts (GEO_01_PUBLIC_IP, GEO_02_GEO_DATA)
    // =========================================================================

    @Test
    fun givenPublicIpEndpoint_whenResponseReceived_thenValidatesIpFormat() = runTest {
        val ipApi = retrofit.create<PublicIpApi>()
        enqueue(200, """{"ip": "203.0.113.10"}""")

        val result = ipApi.getIpAddress()
        assertNotNull(result.ip)
        assertTrue(result.ip!!.matches(Regex("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$")))
    }

    @Test
    fun givenGeoDataEndpoint_whenTokenReturned_thenDecodesBase64Currency() = runTest {
        val geoApi = retrofit.create<GeoRestrictionApi>()
        val encodedToken = Base64.getEncoder().encodeToString("""{"currency": "USD"}""".toByteArray())
        enqueue(200, """{"error": false, "message": "Success", "response": {"geo_token": "$encodedToken"}}""")

        val response = geoApi.getGeoRestrictionAccessKey("203.0.113.10")
        val token = response.response?.geoToken
        assertNotNull(token)

        val decodedJson = String(Base64.getDecoder().decode(token))
        val location = Fixtures.json.decodeFromString<GeoLocationDto>(decodedJson)
        assertEquals("USD", location.currency)
    }

    // =========================================================================
    // 2. Auth API Contracts (AUTH_01_EMAIL_LOGIN)
    // =========================================================================

    private fun createAuthRepo(api: AuthApi) = DefaultAuthRepository(
        api = api,
        crypto = CryptoTransport(),
        config = AppConfig(AppEnvironment.DEVELOPMENT, "https://api.test/", "https://images.test/", true, true, "1.0", TransportCrypto.TEST_PASSPHRASE),
        json = Fixtures.json,
        deviceName = object : DeviceNameProvider { override val deviceName = "Test Pixel" },
        sessions = FakeSessionRepository(),
        ioDispatcher = Dispatchers.Unconfined,
    )

    @Test
    fun givenEmailLoginExistingUser_whenHttp200_thenMapsToCodeSentNotNewUser() = runTest {
        val api = retrofit.create<AuthApi>()
        val repo = createAuthRepo(api)
        enqueue(200, """{"error": false, "statusCode": 200, "status": "success", "message": "OTP sent"}""")

        val result = repo.requestEmailLogin("user@example.com")
        assertTrue(result is EmailLoginResult.CodeSent)
        assertEquals(false, (result as EmailLoginResult.CodeSent).isNewUser)

        val request = server.takeRequest()
        assertEquals("/users/api/v1/auth/email-login", request.url.encodedPath)
        val bodyWrapper = Fixtures.json.decodeFromString<DataPropertiesDto>(request.body!!.utf8())
        assertTrue(bodyWrapper.data.startsWith(CryptoTransport.OPENSSL_SALT_HEADER))
    }

    @Test
    fun givenEmailLoginNewUser_whenHttp203_thenMapsToCodeSentNewUser() = runTest {
        val api = retrofit.create<AuthApi>()
        val repo = createAuthRepo(api)
        enqueue(203, """{"statusCode": 203, "status": "new_user", "message": "New user OTP sent"}""")

        val result = repo.requestEmailLogin("newuser@example.com")
        assertTrue(result is EmailLoginResult.CodeSent)
        assertEquals(true, (result as EmailLoginResult.CodeSent).isNewUser)
    }

    @Test
    fun givenEmailLoginDeviceLimit_whenHttp202_thenExtractsDevicesList() = runTest {
        val api = retrofit.create<AuthApi>()
        val repo = createAuthRepo(api)
        val responseBody = """
            {
              "statusCode": 202,
              "status": "device_limit",
              "message": "Maximum devices reached",
              "response": {
                "device_limit": "2",
                "get_devices": [
                  {
                    "username": "user@example.com",
                    "access_token": "token-1",
                    "device_name": "Chrome on Mac",
                    "device_type": "web",
                    "browser_name": "Chrome"
                  }
                ]
              }
            }
        """.trimIndent()
        enqueue(202, responseBody)

        val result = repo.requestEmailLogin("user@example.com")
        assertTrue(result is EmailLoginResult.DeviceLimitReached)
        val limit = (result as EmailLoginResult.DeviceLimitReached).limit
        assertNotNull(limit)
        assertEquals(2, limit!!.maxDevices)
        assertEquals(1, limit.devices.size)
        assertEquals("Chrome on Mac", limit.devices.first().name)
    }

    @Test
    fun givenEmailLoginHttp200WithLogicalError_thenRejectsNavigation() = runTest {
        val api = retrofit.create<AuthApi>()
        val repo = createAuthRepo(api)
        enqueue(200, """{"error": true, "statusCode": 200, "message": "Account suspended"}""")

        val result = repo.requestEmailLogin("suspended@example.com")
        assertTrue(result is EmailLoginResult.Rejected)
        assertEquals("Account suspended", (result as EmailLoginResult.Rejected).message)
    }

    // =========================================================================
    // 3. Verify OTP Contracts (AUTH_02_VERIFY_EMAIL_OTP)
    // =========================================================================

    @Test
    fun givenValidOtp_whenHttp200WithAccessToken_thenCreatesUserSession() = runTest {
        val api = retrofit.create<AuthApi>()
        val repo = createAuthRepo(api)
        val userResponse = """
            {
              "error": false,
              "statusCode": 200,
              "status": "success",
              "response": {
                "_id": "user-123",
                "email": "user@example.com",
                "first_name": "Alex",
                "access_token": "jwt-access-token",
                "refresh_token": "jwt-refresh-token",
                "default_profile": { "_id": "prof-456" }
              }
            }
        """.trimIndent()
        enqueue(200, userResponse)

        val result = repo.verifyEmailOtp("user@example.com", "123456")
        assertTrue(result is OtpVerificationResult.Success)
        val success = result as OtpVerificationResult.Success
        assertEquals("user-123", success.session.userId)
        assertEquals("jwt-access-token", success.session.accessToken)
        assertEquals("prof-456", success.session.profileId)
    }

    @Test
    fun givenWrongOtp_whenHttp200WithErrorTrue_thenReturnsRejectedForCardShake() = runTest {
        val api = retrofit.create<AuthApi>()
        val repo = createAuthRepo(api)
        enqueue(200, """{"error": true, "statusCode": 200, "message": "Incorrect OTP"}""")

        val result = repo.verifyEmailOtp("user@example.com", "999999")
        assertTrue(result is OtpVerificationResult.Rejected)
        assertEquals("Incorrect OTP", (result as OtpVerificationResult.Rejected).message)
    }

    // =========================================================================
    // 4. Home API Contracts (HOME_01_BANNERS, HOME_02_ROWS)
    // =========================================================================

    @Test
    fun givenHomeBannersEndpoint_whenHttp200_thenMapsHeroBannersCorrectly() = runTest {
        val homeApi = retrofit.create<HomeApi>()
        val repo = DefaultHomeRepository(homeApi, HomeMapper("https://images.test/"), Dispatchers.Unconfined)
        enqueue(200, Fixtures.read("home/banners.json"))

        val banners = repo.getBanners()
        assertTrue(banners.isNotEmpty())
        val first = banners.first()
        assertNotNull(first.id)
        assertNotNull(first.title)
    }

    @Test
    fun givenHomeRowsEndpoint_whenHttp200_thenParsesSectionTypesAndItems() = runTest {
        val homeApi = retrofit.create<HomeApi>()
        val repo = DefaultHomeRepository(homeApi, HomeMapper("https://images.test/"), Dispatchers.Unconfined)
        enqueue(200, Fixtures.read("home/home_page1.json"))

        val result = repo.getSections(page = 1)
        assertTrue(result is com.vplayed.core.error.DataResult.Success)
        val page = (result as com.vplayed.core.error.DataResult.Success).data
        assertEquals(2, page.sections.size)
        assertEquals(SectionStyle.LIVE, page.sections[0].style)
        assertEquals(SectionStyle.LANDSCAPE, page.sections[1].style)
    }
}
