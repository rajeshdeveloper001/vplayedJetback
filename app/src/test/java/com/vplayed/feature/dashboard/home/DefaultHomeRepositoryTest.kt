package com.vplayed.feature.dashboard.home

import com.vplayed.core.error.AppError
import com.vplayed.core.error.DataResult
import com.vplayed.feature.dashboard.home.data.DefaultHomeRepository
import com.vplayed.feature.dashboard.home.data.mapper.HomeMapper
import com.vplayed.feature.dashboard.home.data.remote.HomeApi
import com.vplayed.testing.Fixtures
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create

/** Real Retrofit + JSON configuration against a local server serving synthetic fixtures. */
class DefaultHomeRepositoryTest {

    private val server = MockWebServer()
    private lateinit var repository: DefaultHomeRepository

    @Before
    fun setUp() {
        server.start()
        val api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(Fixtures.json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create<HomeApi>()
        repository = DefaultHomeRepository(api, HomeMapper("https://images.test/"), Dispatchers.Unconfined)
    }

    @After
    fun tearDown() = server.close()

    private fun enqueue(fixture: String, code: Int = 200) =
        server.enqueue(MockResponse.Builder().code(code).body(Fixtures.read(fixture)).build())

    private fun enqueueBody(body: String, code: Int) = server.enqueue(MockResponse.Builder().code(code).body(body).build())

    @Test
    fun givenFirstPage_whenLoadingSections_thenRequestsPagedEndpointAndMapsRows() = runTest {
        enqueue("home/home_page1.json")

        val result = repository.getSections(page = 1)

        val request = server.takeRequest()
        assertEquals("/mediaview/api/v1/home", request.url.encodedPath)
        assertEquals("1", request.url.queryParameter("page"))
        assertEquals("3", request.url.queryParameter("limit"))
        assertEquals("10", request.url.queryParameter("inside_limit"))
        val page = (result as DataResult.Success).data
        assertEquals(listOf("sec-live", "sec-videos"), page.sections.map { it.id })
        assertEquals(2, page.nextPage)
    }

    @Test
    fun givenLastPage_whenLoadingSections_thenNextPageIsNull() = runTest {
        enqueue("home/home_page3.json")
        val page = (repository.getSections(page = 3) as DataResult.Success).data
        assertEquals(null, page.nextPage)
    }

    @Test
    fun givenBannersWithGenreIds_whenLoading_thenParsesInsteadOfFailing() = runTest {
        enqueue("home/banners.json")

        val result = repository.getBanners()

        assertEquals("/mediaview/api/v1/homebanners", server.takeRequest().url.encodedPath)
        assertEquals(2, (result as DataResult.Success).data.size)
    }

    @Test
    fun givenRowPaging_whenLoadingMoreItems_thenRequestsRowEndpoint() = runTest {
        enqueue("home/section_items_page2.json")

        val result = repository.getSectionItems("sec-videos", page = 2)

        val request = server.takeRequest()
        assertEquals("/mediaview/api/v1/home/all/sec-videos", request.url.encodedPath)
        assertEquals("2", request.url.queryParameter("page"))
        assertEquals("10", request.url.queryParameter("inside_limit"))
        assertEquals(3, (result as DataResult.Success).data.nextPage)
    }

    @Test
    fun givenServerError_whenLoading_thenReturnsServerError() = runTest {
        enqueueBody("{}", code = 500)
        val error = (repository.getSections(1) as DataResult.Failure).error
        assertEquals(500, (error as AppError.Server).code)
    }

    @Test
    fun givenUnauthorized_whenLoading_thenReturnsUnauthorized() = runTest {
        enqueueBody("{}", code = 401)
        assertEquals(DataResult.Failure(AppError.Unauthorized), repository.getSections(1))
    }

    @Test
    fun givenFailedEnvelope_whenLoading_thenReturnsServerErrorWithApiMessage() = runTest {
        enqueue("home/failed_envelope.json")
        assertEquals(DataResult.Failure(AppError.Server(422, "Invalid page")), repository.getSections(1))
    }

    @Test
    fun givenMalformedJson_whenLoading_thenReturnsParsingError() = runTest {
        enqueueBody("{ not json", code = 200)
        assertEquals(DataResult.Failure(AppError.Parsing), repository.getSections(1))
    }

    @Test
    fun givenNoConnection_whenLoading_thenReturnsNetworkError() = runTest {
        server.close()
        val result = repository.getSections(1)
        assertTrue(result is DataResult.Failure && result.error == AppError.Network)
    }
}
