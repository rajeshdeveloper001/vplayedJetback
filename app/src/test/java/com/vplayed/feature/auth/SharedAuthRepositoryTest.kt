package com.vplayed.feature.auth

import com.vplayed.core.common.config.AppConfig
import com.vplayed.core.common.config.AppEnvironment
import com.vplayed.core.error.AppError
import com.vplayed.core.geo.GeoRestrictionStore
import com.vplayed.core.network.DeviceIdProvider
import com.vplayed.core.network.DeviceNameProvider
import com.vplayed.core.session.UserSession
import com.vplayed.core.shared.AndroidRequestCredentials
import com.vplayed.core.shared.AndroidSharedDeviceInfo
import com.vplayed.core.shared.toSharedConfig
import com.vplayed.feature.auth.data.SharedAuthRepository
import com.vplayed.feature.auth.domain.EmailLoginResult
import com.vplayed.feature.auth.domain.model.DeviceKind
import com.vplayed.feature.auth.domain.model.DeviceSignOutKey
import com.vplayed.feature.auth.domain.model.OtpVerificationResult
import com.vplayed.feature.auth.domain.model.SignOutDeviceResult
import com.vplayed.shared.PlatformDependencies
import com.vplayed.shared.SharedGraph
import com.vplayed.shared.core.crypto.platformCryptoPrimitives
import com.vplayed.shared.core.network.createOkHttpEngine
import com.vplayed.shared.core.platform.NoOpLogger
import com.vplayed.testing.FakeSessionRepository
import com.vplayed.testing.TransportCrypto
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The app's [com.vplayed.feature.auth.domain.AuthRepository] on top of the KMP shared layer: the
 * real shared client (OkHttp engine) answered by a terminal interceptor. The status-code rules
 * themselves are tested in the shared module (`AuthRepositoryTest`); this checks the wiring, the
 * mapping to the app's types and the stored session.
 */
class SharedAuthRepositoryTest {

    private val sessions = FakeSessionRepository()
    private val geoStore = GeoRestrictionStore()
    private val answers = ArrayDeque<Pair<Int, String>>()
    private val requests = mutableListOf<Request>()
    private var dropConnection = false

    private val backend = Interceptor { chain ->
        requests += chain.request()
        if (dropConnection) throw IOException("connection reset")
        val (code, body) = answers.removeFirst()
        Response.Builder()
            .request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("")
            .body(body.toResponseBody("application/json".toMediaType()))
            .build()
    }

    private fun repository(transportKey: String = TransportCrypto.TEST_PASSPHRASE): SharedAuthRepository {
        val config = AppConfig(AppEnvironment.DEVELOPMENT, "https://api.test/", "https://images.test/", true, true, "1.0", transportKey)
        val graph = SharedGraph(
            config.toSharedConfig(),
            PlatformDependencies(
                deviceInfo = AndroidSharedDeviceInfo(
                    object : DeviceIdProvider {
                        override val deviceId: String? = "device-123"
                    },
                    object : DeviceNameProvider {
                        override val deviceName = "Test Pixel"
                    },
                ),
                logger = NoOpLogger,
                engineFactory = { createOkHttpEngine(OkHttpClient.Builder().addInterceptor(backend).build()) },
                cryptoPrimitives = platformCryptoPrimitives(),
                requestCredentials = AndroidRequestCredentials(sessions, geoStore),
            ),
        )
        return SharedAuthRepository(graph, sessions, Dispatchers.Unconfined)
    }

    private fun answer(code: Int, body: String) = answers.addLast(code to body)

    private fun Request.bodyText(): String = Buffer().also { body!!.writeTo(it) }.readUtf8()

    @Test
    fun givenEmail_whenRequesting_thenSharedLayerPostsEncryptedEmailWithAppHeaders() = runTest {
        geoStore.updateGeo("geo-xyz", null)
        answer(200, """{"error":false,"statusCode":200,"message":"OTP sent"}""")

        val result = repository().requestEmailLogin(" user@example.com ")

        assertEquals(EmailLoginResult.CodeSent("OTP sent", isNewUser = false), result)
        val request = requests.single()
        assertEquals("/users/api/v1/auth/email-login", request.url.encodedPath)
        assertEquals("android", request.header("device_type"))
        assertEquals("device-123", request.header("device_id"))
        // Read live from the app's geo store, as ApiHeadersInterceptor does.
        assertEquals("geo-xyz", request.header("x-bearer-token"))
        val data = Json.parseToJsonElement(request.bodyText()).jsonObject.getValue("data").jsonPrimitive.content
        assertEquals("""{"email":"user@example.com"}""", TransportCrypto.decrypt(TransportCrypto.TEST_PASSPHRASE, data))
    }

    @Test
    fun givenNewAccount_whenRequesting_thenCodeSentToNewUser() = runTest {
        answer(203, """{"statusCode":203,"message":"Welcome"}""")
        assertEquals(EmailLoginResult.CodeSent("Welcome", isNewUser = true), repository().requestEmailLogin(EMAIL))
    }

    @Test
    fun givenCorrectCode_whenVerified_thenAppSessionSaved() = runTest {
        answer(
            200,
            """{"error":false,"statusCode":200,"response":{"_id":"u-1","email":"user@example.com","first_name":"Sam",
            "access_token":"access-1","refresh_token":"refresh-1","default_profile":{"_id":"p-1"},"profile_count":"3"}}""",
        )

        val result = repository().verifyEmailOtp(EMAIL, "123456")

        assertEquals(OtpVerificationResult.Verified(isNewUser = false, hasMultipleProfiles = true), result)
        assertEquals(listOf(UserSession("u-1", EMAIL, "Sam", "access-1", "refresh-1", "p-1")), sessions.saved)
    }

    @Test
    fun givenSessionWithoutIdOrEmail_whenVerified_thenRequestedEmailIdentifiesTheUser() = runTest {
        answer(200, """{"statusCode":200,"response":{"access_token":"access-1"}}""")

        repository().verifyEmailOtp(" user@example.com ", "123456")

        assertEquals(UserSession(EMAIL, EMAIL, null, "access-1", null, null), sessions.saved.single())
    }

    @Test
    fun givenRefusalOrDeviceLimit_whenVerifying_thenNoSession() = runTest {
        answer(422, """{"error":true,"statusCode":422,"message":"OTP is invalid"}""")
        assertEquals(OtpVerificationResult.Rejected("OTP is invalid"), repository().verifyEmailOtp(EMAIL, "111111"))
        answer(203, """{"statusCode":203,"message":"Please register"}""")
        assertEquals(OtpVerificationResult.RegistrationRequired("Please register"), repository().verifyEmailOtp(EMAIL, "111111"))
        answer(
            202,
            """{"statusCode":202,"message":"Device limit exceeded","response":{"device_limit":2,"get_devices":[
            {"username":"dev-user-1","access_token":"dev-token-1","device_name":"Android TV","device_type":"android tv","current_device":1}]}}""",
        )
        val limit = (repository().verifyEmailOtp(EMAIL, "123456") as OtpVerificationResult.DeviceLimitReached).limit!!

        assertEquals(2, limit.maxDevices)
        assertEquals(DeviceKind.TV, limit.devices.single().kind)
        assertTrue(limit.devices.single().isCurrent)
        assertEquals(DeviceSignOutKey("dev-user-1", "dev-token-1"), limit.devices.single().signOutKey)
        assertTrue(sessions.saved.isEmpty())
    }

    @Test
    fun givenFailures_whenCalling_thenMappedToAppErrors() = runTest {
        answer(500, "Internal Server Error")
        assertEquals(EmailLoginResult.Failure(AppError.Server(500)), repository().requestEmailLogin(EMAIL))
        answer(401, "")
        assertEquals(EmailLoginResult.Failure(AppError.Unauthorized), repository().requestEmailLogin(EMAIL))
        answer(200, "<html>")
        assertEquals(OtpVerificationResult.Failure(AppError.Parsing), repository().verifyEmailOtp(EMAIL, "123456"))

        dropConnection = true
        assertEquals(EmailLoginResult.Failure(AppError.Network), repository().requestEmailLogin(EMAIL))
    }

    @Test
    fun givenNoTransportKey_whenRequesting_thenUnknownFailureWithoutRequest() = runTest {
        val result = repository(transportKey = "").requestEmailLogin(EMAIL)

        assertTrue((result as EmailLoginResult.Failure).error is AppError.Unknown)
        assertTrue(requests.isEmpty())
    }

    @Test
    fun givenDeviceStep_whenSigningOutAndContinuing_thenFormsSentAndSessionSaved() = runTest {
        answer(200, """{"error":false}""")
        assertEquals(SignOutDeviceResult.Success, repository().signOutDevice(DeviceSignOutKey("dev-user-1", "dev-token-1"), all = true))
        assertEquals("username=dev-user-1&access_token=dev-token-1&preference=all", requests.last().bodyText())

        answer(200, """{"statusCode":200,"response":{"_id":"u-1","access_token":"access-1"}}""")
        assertTrue(repository().continueAfterDeviceSignOut(EMAIL) is OtpVerificationResult.Verified)
        assertEquals("/users/api/v1/auth/verifyEmailLogin", requests.last().url.encodedPath)
        assertEquals("access-1", sessions.saved.single().accessToken)
    }

    private companion object {
        const val EMAIL = "user@example.com"
    }
}
