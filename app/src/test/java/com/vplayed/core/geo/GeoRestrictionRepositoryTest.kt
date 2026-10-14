package com.vplayed.core.geo

import com.vplayed.core.geo.data.GeoAccessKeyDto
import com.vplayed.core.geo.data.GeoAccessKeyResponseDto
import com.vplayed.core.geo.data.GeoRestrictionApi
import com.vplayed.core.geo.data.PublicIpApi
import com.vplayed.core.geo.data.PublicIpDto
import com.vplayed.testing.Fixtures
import java.io.IOException
import kotlin.io.encoding.Base64
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create

class GeoRestrictionRepositoryTest {

    private val store = GeoRestrictionStore()
    private val ipApi = FakePublicIpApi()
    private val geoApi = FakeGeoRestrictionApi()
    private val repository = GeoRestrictionRepository(ipApi, geoApi, GeoTokenDecoder(Fixtures.json), store)

    @Test
    fun givenIpAndToken_whenRefreshing_thenStoreHoldsIpTokenAndCurrency() = runTest {
        val outcome = repository.refresh()

        assertEquals(GeoRefreshOutcome.Success, outcome)
        assertEquals(listOf(TEST_IP), geoApi.requestedIps)
        assertEquals(GeoRestrictionState(TEST_IP, USD_TOKEN, "USD"), store.state.value)
    }

    @Test
    fun givenIpRequestFails_whenRefreshing_thenTransientFailureAndStateUnchanged() = runTest {
        ipApi.failure = IOException("offline")

        val outcome = repository.refresh()

        assertEquals(GeoRefreshOutcome.TransientFailure, outcome)
        assertEquals(GeoRestrictionState(), store.state.value)
        assertEquals(emptyList<String>(), geoApi.requestedIps)
    }

    @Test
    fun givenInvalidIp_whenRefreshing_thenInvalidResponseAndNoGeoRequest() = runTest {
        ipApi.ip = "not an ip"
        assertEquals(GeoRefreshOutcome.InvalidResponse, repository.refresh())
        ipApi.ip = null
        assertEquals(GeoRefreshOutcome.InvalidResponse, repository.refresh())
        assertEquals(emptyList<String>(), geoApi.requestedIps)
        assertNull(store.state.value.ip)
    }

    @Test
    fun givenPreviousValidState_whenGeoRequestFails_thenTokenAndCurrencyPreserved() = runTest {
        repository.refresh()
        geoApi.failure = HttpException(Response.error<Any>(503, "".toResponseBody()))

        val outcome = repository.refresh()

        assertEquals(GeoRefreshOutcome.TransientFailure, outcome)
        assertEquals(USD_TOKEN, store.state.value.geoToken)
        assertEquals("USD", store.state.value.currency)
    }

    @Test
    fun givenPreviousValidState_whenTokenMissingOrBlank_thenPreviousStatePreserved() = runTest {
        repository.refresh()

        geoApi.token = null
        assertEquals(GeoRefreshOutcome.InvalidResponse, repository.refresh())
        geoApi.token = "  "
        assertEquals(GeoRefreshOutcome.InvalidResponse, repository.refresh())

        assertEquals(USD_TOKEN, store.state.value.geoToken)
        assertEquals("USD", store.state.value.currency)
    }

    @Test
    fun givenClientError_whenRefreshing_thenInvalidResponseNotRetried() = runTest {
        geoApi.failure = HttpException(Response.error<Any>(404, "".toResponseBody()))
        assertEquals(GeoRefreshOutcome.InvalidResponse, repository.refresh())
    }

    @Test
    fun givenUndecodableToken_whenRefreshing_thenTokenSavedAndCurrencyStaysDefault() = runTest {
        geoApi.token = "%%%not-base64%%%"

        assertEquals(GeoRefreshOutcome.Success, repository.refresh())

        assertEquals("%%%not-base64%%%", store.state.value.geoToken)
        assertEquals(GeoRestrictionState.DEFAULT_CURRENCY, store.state.value.currency)
    }

    @Test
    fun givenConcurrentRefreshes_whenRunning_thenRequestsNeverOverlap() = runTest {
        val gate = CompletableDeferred<Unit>()
        ipApi.gate = gate

        val calls = List(3) { async { repository.refresh() } }
        yield()
        gate.complete(Unit)
        val outcomes = calls.awaitAll()

        assertEquals(List(3) { GeoRefreshOutcome.Success }, outcomes)
        assertEquals(1, ipApi.maxConcurrentCalls)
    }

    @Test
    fun givenRealGeoApi_whenRequesting_thenCallsGeoDataPathWithIp() = runTest {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(MockResponse.Builder().body("""{"error":false,"response":{"geo_token":"$USD_TOKEN"}}""").build())
            val api = Retrofit.Builder()
                .baseUrl(server.url("/"))
                .addConverterFactory(Fixtures.json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create<GeoRestrictionApi>()

            val response = api.getGeoRestrictionAccessKey(TEST_IP)

            assertEquals("/mediaview/api/v1/common/geo-data/$TEST_IP", server.takeRequest().url.encodedPath)
            assertEquals(USD_TOKEN, response.response?.geoToken)
        }
    }

    private class FakePublicIpApi : PublicIpApi {
        var ip: String? = TEST_IP
        var failure: Exception? = null
        var gate: CompletableDeferred<Unit>? = null
        var maxConcurrentCalls = 0
        private var running = 0

        override suspend fun getIpAddress(): PublicIpDto {
            running++
            maxConcurrentCalls = maxOf(maxConcurrentCalls, running)
            try {
                gate?.await()
                failure?.let { throw it }
                return PublicIpDto(ip)
            } finally {
                running--
            }
        }
    }

    private class FakeGeoRestrictionApi : GeoRestrictionApi {
        var token: String? = USD_TOKEN
        var failure: Exception? = null
        val requestedIps = mutableListOf<String>()

        override suspend fun getGeoRestrictionAccessKey(ip: String): GeoAccessKeyResponseDto {
            requestedIps += ip
            failure?.let { throw it }
            return GeoAccessKeyResponseDto(GeoAccessKeyDto(token))
        }
    }

    private companion object {
        // Documentation range address (RFC 5737); no real user IPs in tests.
        const val TEST_IP = "203.0.113.10"
        val USD_TOKEN = Base64.encode("""{"currency":"USD"}""".encodeToByteArray())
    }
}
