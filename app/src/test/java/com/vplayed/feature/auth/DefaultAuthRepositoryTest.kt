package com.vplayed.feature.auth

import com.vplayed.core.common.config.AppConfig
import com.vplayed.core.common.config.AppEnvironment
import com.vplayed.core.error.AppError
import com.vplayed.core.network.DeviceNameProvider
import com.vplayed.core.security.CryptoTransport
import com.vplayed.core.session.UserSession
import com.vplayed.feature.auth.data.DefaultAuthRepository
import com.vplayed.feature.auth.data.remote.AuthApi
import com.vplayed.feature.auth.domain.EmailLoginResult
import com.vplayed.feature.auth.domain.model.DeviceKind
import com.vplayed.feature.auth.domain.model.DeviceSignOutKey
import com.vplayed.feature.auth.domain.model.OtpVerificationResult
import com.vplayed.feature.auth.domain.model.SignOutDeviceResult
import com.vplayed.testing.FakeSessionRepository
import com.vplayed.testing.Fixtures
import com.vplayed.testing.TransportCrypto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.SocketEffect
import okhttp3.MediaType.Companion.toMediaType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create

/** Real Retrofit + JSON + encryption against a local server with synthetic responses. */
class DefaultAuthRepositoryTest {

    private val server = MockWebServer()
    private lateinit var api: AuthApi
    private val sessions = FakeSessionRepository()

    @Before
    fun setUp() {
        server.start()
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(Fixtures.json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create<AuthApi>()
    }

    @After
    fun tearDown() = server.close()

    private fun repository(transportKey: String = TransportCrypto.TEST_PASSPHRASE) = DefaultAuthRepository(
        api = api,
        crypto = CryptoTransport(),
        config = AppConfig(AppEnvironment.DEVELOPMENT, "https://api.test/", "https://images.test/", true, true, "1.0", transportKey),
        json = Fixtures.json,
        deviceName = object : DeviceNameProvider {
            override val deviceName = "Test Pixel"
        },
        sessions = sessions,
        ioDispatcher = Dispatchers.Unconfined,
    )

    private fun enqueue(code: Int, body: String) =
        server.enqueue(MockResponse.Builder().code(code).body(body).build())

    @Test
    fun givenValidEmail_whenRequesting_thenPostsEncryptedDataWrapperOnce() = runTest {
        enqueue(200, """{"error":false,"statusCode":200,"status":"success","message":"OTP sent"}""")

        repository().requestEmailLogin("  user@example.com  ")

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/users/api/v1/auth/email-login", request.url.encodedPath)
        val body = Fixtures.json.parseToJsonElement(request.body!!.utf8()).jsonObject
        assertEquals(setOf("data"), body.keys)
        val cipherText = body.getValue("data").jsonPrimitive.content
        assertTrue("Raw email must not be sent", "user@example.com" !in request.body!!.utf8())
        // Decrypting once yields the plain email JSON, so it was encrypted exactly once.
        assertEquals("""{"email":"user@example.com"}""", TransportCrypto.decrypt(TransportCrypto.TEST_PASSPHRASE, cipherText))
    }

    @Test
    fun given200_whenRequesting_thenCodeSentToExistingUser() = runTest {
        enqueue(200, """{"error":false,"statusCode":200,"message":"OTP sent to your email"}""")
        assertEquals(EmailLoginResult.CodeSent("OTP sent to your email", isNewUser = false), repository().requestEmailLogin(EMAIL))
    }

    @Test
    fun given203_whenRequesting_thenCodeSentToNewUser() = runTest {
        enqueue(203, """{"error":false,"statusCode":203,"message":"Welcome"}""")
        assertEquals(EmailLoginResult.CodeSent("Welcome", isNewUser = true), repository().requestEmailLogin(EMAIL))
    }

    @Test
    fun given202_whenRequesting_thenDeviceLimitReached() = runTest {
        enqueue(202, """{"error":true,"statusCode":202,"message":"Device limit exceeded"}""")
        assertEquals(EmailLoginResult.DeviceLimitReached("Device limit exceeded"), repository().requestEmailLogin(EMAIL))
    }

    @Test
    fun givenHttp200WithApiError_whenRequesting_thenRejectedNotSuccess() = runTest {
        enqueue(200, """{"error":true,"statusCode":422,"message":"Account blocked"}""")
        assertEquals(EmailLoginResult.Rejected("Account blocked"), repository().requestEmailLogin(EMAIL))
    }

    @Test
    fun given203WithErrorFlag_whenRequesting_thenStatusDecidesAndCodeIsSent() = runTest {
        // The previous app opens the OTP screen on 203 without reading the body's `error` flag.
        enqueue(203, """{"error":true,"statusCode":203,"message":"Welcome"}""")
        assertEquals(EmailLoginResult.CodeSent("Welcome", isNewUser = true), repository().requestEmailLogin(EMAIL))
    }

    @Test
    fun givenUnexpected2xx_whenRequesting_thenNotReportedAsSuccess() = runTest {
        enqueue(201, """{"error":false,"message":"?"}""")
        assertTrue(repository().requestEmailLogin(EMAIL) is EmailLoginResult.Failure)
    }

    @Test
    fun givenEmptyOrMalformedBody_whenRequesting_thenParsingFailure() = runTest {
        enqueue(200, "")
        assertEquals(EmailLoginResult.Failure(AppError.Parsing), repository().requestEmailLogin(EMAIL))
        enqueue(200, "<html>oops</html>")
        assertEquals(EmailLoginResult.Failure(AppError.Parsing), repository().requestEmailLogin(EMAIL))
    }

    @Test
    fun givenClientErrorWithMessage_whenRequesting_thenRejectedWithServerMessage() = runTest {
        enqueue(422, """{"error":true,"statusCode":422,"message":"The email must be a valid email address."}""")
        assertEquals(
            EmailLoginResult.Rejected("The email must be a valid email address."),
            repository().requestEmailLogin(EMAIL),
        )
    }

    @Test
    fun givenServerErrorWithoutEnvelope_whenRequesting_thenServerFailure() = runTest {
        enqueue(500, "Internal Server Error")
        assertEquals(EmailLoginResult.Failure(AppError.Server(500)), repository().requestEmailLogin(EMAIL))
    }

    @Test
    fun givenUnauthorizedWithoutMessage_whenRequesting_thenUnauthorizedFailure() = runTest {
        enqueue(401, "")
        assertEquals(EmailLoginResult.Failure(AppError.Unauthorized), repository().requestEmailLogin(EMAIL))
    }

    @Test
    fun givenConnectionDrop_whenRequesting_thenNetworkFailure() = runTest {
        server.enqueue(MockResponse.Builder().onRequestStart(SocketEffect.CloseSocket()).build())
        assertEquals(EmailLoginResult.Failure(AppError.Network), repository().requestEmailLogin(EMAIL))
    }

    @Test
    fun givenNoTransportKey_whenRequesting_thenFailsWithoutSendingRequest() = runTest {
        val result = repository(transportKey = "").requestEmailLogin(EMAIL)

        assertTrue((result as EmailLoginResult.Failure).error is AppError.Unknown)
        assertEquals(0, server.requestCount)
    }

    // region verifyEmailOtp

    private fun verifiedBody(error: String = "false", profileCount: String = "1", firstName: String = "Sam") =
        """{"error":$error,"statusCode":200,"message":"Login successful","response":{"_id":"u-1","email":"user@example.com",
        "first_name":"$firstName","access_token":"access-1","refresh_token":"refresh-1","default_profile":{"_id":"p-1"},
        "profile_count":$profileCount,"isNewUsers":false}}"""

    @Test
    fun givenCorrectCode_whenVerifying_thenPostsEncryptedEmailCodeAndDeviceName() = runTest {
        enqueue(200, verifiedBody())

        repository().verifyEmailOtp("  user@example.com ", "123456")

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/users/api/v1/auth/verifyEmailOtp", request.url.encodedPath)
        val body = Fixtures.json.parseToJsonElement(request.body!!.utf8()).jsonObject
        assertEquals(setOf("data"), body.keys)
        val plain = TransportCrypto.decrypt(TransportCrypto.TEST_PASSPHRASE, body.getValue("data").jsonPrimitive.content)
        assertEquals("""{"email":"user@example.com","otp":"123456","deviceDetail":"Test Pixel"}""", plain)
        assertTrue("The code must not be sent in clear", "123456" !in request.body!!.utf8())
    }

    @Test
    fun givenCorrectCode_whenVerified_thenSessionSavedAndResultVerified() = runTest {
        enqueue(200, verifiedBody())

        val result = repository().verifyEmailOtp(EMAIL, "123456")

        assertEquals(OtpVerificationResult.Verified(isNewUser = false, hasMultipleProfiles = false), result)
        assertEquals(
            listOf(UserSession("u-1", "user@example.com", "Sam", "access-1", "refresh-1", "p-1")),
            sessions.saved,
        )
    }

    @Test
    fun givenAccountWithSeveralProfilesOrNoName_whenVerified_thenFlagsReflectIt() = runTest {
        enqueue(200, verifiedBody(profileCount = "\"3\"", firstName = ""))

        val result = repository().verifyEmailOtp(EMAIL, "123456")

        assertEquals(OtpVerificationResult.Verified(isNewUser = true, hasMultipleProfiles = true), result)
    }

    @Test
    fun givenHttp200WithApiError_whenVerifying_thenRejectedAndNoSession() = runTest {
        enqueue(200, verifiedBody(error = "true").replace("Login successful", "Invalid OTP"))

        val result = repository().verifyEmailOtp(EMAIL, "123456")

        assertEquals(OtpVerificationResult.Rejected("Invalid OTP"), result)
        assertTrue(sessions.saved.isEmpty())
    }

    @Test
    fun givenHttp200WithoutToken_whenVerifying_thenFailureNotLogin() = runTest {
        enqueue(200, """{"error":false,"message":"ok","response":{"_id":"u-1"}}""")
        assertEquals(OtpVerificationResult.Failure(AppError.Parsing), repository().verifyEmailOtp(EMAIL, "123456"))
        enqueue(200, """{"error":false,"message":"ok"}""")
        assertEquals(OtpVerificationResult.Failure(AppError.Parsing), repository().verifyEmailOtp(EMAIL, "123456"))
        enqueue(200, "")
        assertEquals(OtpVerificationResult.Failure(AppError.Parsing), repository().verifyEmailOtp(EMAIL, "123456"))
        assertTrue(sessions.saved.isEmpty())
    }

    @Test
    fun givenInvalidOrExpiredCode_whenServerRefuses_thenRejectedWithServerMessage() = runTest {
        enqueue(422, """{"error":true,"statusCode":422,"message":"OTP is invalid"}""")
        assertEquals(OtpVerificationResult.Rejected("OTP is invalid"), repository().verifyEmailOtp(EMAIL, "111111"))
        enqueue(400, """{"error":true,"statusCode":400,"message":"OTP has expired"}""")
        assertEquals(OtpVerificationResult.Rejected("OTP has expired"), repository().verifyEmailOtp(EMAIL, "222222"))
        assertTrue(sessions.saved.isEmpty())
    }

    @Test
    fun givenServerError_whenVerifying_thenServerFailure() = runTest {
        enqueue(500, "Internal Server Error")
        assertEquals(OtpVerificationResult.Failure(AppError.Server(500)), repository().verifyEmailOtp(EMAIL, "123456"))
    }

    @Test
    fun givenConnectionDrop_whenVerifying_thenNetworkFailure() = runTest {
        server.enqueue(MockResponse.Builder().onRequestStart(SocketEffect.CloseSocket()).build())
        assertEquals(OtpVerificationResult.Failure(AppError.Network), repository().verifyEmailOtp(EMAIL, "123456"))
    }

    @Test
    fun givenNewAccountWithoutSession_whenVerifying_thenRegistrationRequired() = runTest {
        enqueue(203, """{"error":false,"statusCode":203,"message":"Please register"}""")
        assertEquals(OtpVerificationResult.RegistrationRequired("Please register"), repository().verifyEmailOtp(EMAIL, "123456"))
        assertTrue(sessions.saved.isEmpty())
    }

    @Test
    fun given203WithErrorFlagAndNoSession_whenVerifying_thenRegistrationRequiredNotRejected() = runTest {
        enqueue(203, """{"error":true,"statusCode":203,"message":"Please register"}""")
        assertEquals(OtpVerificationResult.RegistrationRequired("Please register"), repository().verifyEmailOtp(EMAIL, "123456"))
    }

    @Test
    fun givenNewAccountWithSession_whenVerifying_thenSignedInAsNewUser() = runTest {
        enqueue(203, verifiedBody())
        assertEquals(OtpVerificationResult.Verified(isNewUser = true, hasMultipleProfiles = false), repository().verifyEmailOtp(EMAIL, "123456"))
        assertEquals(1, sessions.saved.size)
    }

    @Test
    fun givenDeviceLimit_whenVerifying_thenDevicesListedWithoutSession() = runTest {
        enqueue(202, deviceLimitBody())

        val result = repository().verifyEmailOtp(EMAIL, "123456") as OtpVerificationResult.DeviceLimitReached

        assertEquals("Device limit exceeded", result.message)
        assertEquals(2, result.limit!!.maxDevices)
        assertEquals(listOf("Chrome on Linux", "iPhone 15"), result.limit!!.devices.map { it.name })
        assertEquals(listOf(DeviceKind.DESKTOP, DeviceKind.PHONE), result.limit!!.devices.map { it.kind })
        assertEquals(DeviceSignOutKey("dev-user-1", "dev-token-1"), result.limit!!.devices[0].signOutKey)
        assertTrue(sessions.saved.isEmpty())
    }

    @Test
    fun givenDeviceLimit_whenRequestingCode_thenDevicesListed() = runTest {
        enqueue(202, deviceLimitBody())

        val result = repository().requestEmailLogin(EMAIL) as EmailLoginResult.DeviceLimitReached

        assertEquals(2, result.limit!!.devices.size)
    }

    private fun deviceLimitBody() = """{"error":true,"statusCode":202,"message":"Device limit exceeded","response":{
        "device_limit":"2","get_devices":[
        {"username":"dev-user-1","access_token":"dev-token-1","device_name":"Chrome on Linux","device_type":"web","browser_name":"Chrome","current_device":0},
        {"username":"dev-user-2","access_token":"dev-token-2","device_name":"iPhone 15","device_type":"ios","browser_name":"","current_device":"1"},
        {"device_name":"No credentials","device_type":"web"}]}}"""

    @Test
    fun givenDeviceKeys_whenPrinted_thenCredentialsAreMasked() {
        assertTrue("dev-token" !in DeviceSignOutKey("dev-user", "dev-token").toString())
    }

    // endregion

    // region devices

    @Test
    fun givenOneDevice_whenSigningOut_thenPostsItsOwnCredentialsWithPreferenceOne() = runTest {
        enqueue(200, """{"error":false,"message":"Device logged out"}""")

        val result = repository().signOutDevice(DeviceSignOutKey("dev-user-1", "dev-token-1"), all = false)

        assertEquals(SignOutDeviceResult.Success, result)
        val request = server.takeRequest()
        assertEquals("/users/api/v1/auth/logout-device", request.url.encodedPath)
        assertEquals("username=dev-user-1&access_token=dev-token-1&preference=1", request.body!!.utf8())
    }

    @Test
    fun givenAllDevices_whenSigningOut_thenPreferenceAll() = runTest {
        enqueue(200, """{"error":false}""")
        repository().signOutDevice(DeviceSignOutKey("dev-user-1", "dev-token-1"), all = true)
        assertTrue(server.takeRequest().body!!.utf8().endsWith("preference=all"))
    }

    @Test
    fun givenSignOutRefused_whenSigningOut_thenRejectedOrFailure() = runTest {
        enqueue(200, """{"error":true,"message":"Could not sign out"}""")
        assertEquals(SignOutDeviceResult.Rejected("Could not sign out"), repository().signOutDevice(DeviceSignOutKey("u", "t"), false))
        // Without an explicit `error: false` the previous app does not accept the answer.
        enqueue(200, """{"message":"?"}""")
        assertTrue(repository().signOutDevice(DeviceSignOutKey("u", "t"), false) is SignOutDeviceResult.Rejected)
        enqueue(500, "")
        assertEquals(SignOutDeviceResult.Failure(AppError.Server(500)), repository().signOutDevice(DeviceSignOutKey("u", "t"), false))
    }

    @Test
    fun givenDevicesSignedOut_whenContinuing_thenPostsFormWithoutCodeAndSignsIn() = runTest {
        enqueue(200, verifiedBody())

        val result = repository().continueAfterDeviceSignOut(" user@example.com ")

        assertTrue(result is OtpVerificationResult.Verified)
        val request = server.takeRequest()
        assertEquals("/users/api/v1/auth/verifyEmailLogin", request.url.encodedPath)
        assertEquals(
            "is_email_verified=1&email=user%40example.com&deviceDetail=Test%20Pixel&browsername=",
            request.body!!.utf8().replace("Test+Pixel", "Test%20Pixel"),
        )
        assertEquals(1, sessions.saved.size)
    }

    // endregion

    @Test
    fun givenDeviceTypes_whenMapping_thenIconKind() {
        assertEquals(DeviceKind.PHONE, DefaultAuthRepository.deviceKindOf("android"))
        assertEquals(DeviceKind.TV, DefaultAuthRepository.deviceKindOf("Android TV"))
        assertEquals(DeviceKind.DESKTOP, DefaultAuthRepository.deviceKindOf("Web"))
        assertEquals(DeviceKind.OTHER, DefaultAuthRepository.deviceKindOf(null))
        assertEquals(DeviceKind.OTHER, DefaultAuthRepository.deviceKindOf("toaster"))
    }

    private companion object {
        const val EMAIL = "user@example.com"
    }
}
