package com.vplayed.feature.dashboard.home.data

import com.vplayed.core.dispatcher.DefaultDispatcher
import com.vplayed.core.error.ApiException
import com.vplayed.core.error.DataResult
import com.vplayed.core.error.resultOf
import com.vplayed.feature.dashboard.home.data.mapper.HomeMapper
import com.vplayed.feature.dashboard.home.data.remote.HomeApi
import com.vplayed.feature.dashboard.home.data.remote.dto.ApiEnvelope
import com.vplayed.feature.dashboard.home.domain.HomeRepository
import com.vplayed.feature.dashboard.home.domain.HomeRepository.Companion.ITEMS_PER_SECTION
import com.vplayed.feature.dashboard.home.domain.HomeRepository.Companion.SECTIONS_PER_PAGE
import com.vplayed.feature.dashboard.home.domain.model.HomeBanner
import com.vplayed.feature.dashboard.home.domain.model.HomeSectionsPage
import com.vplayed.feature.dashboard.home.domain.model.SectionItemsPage
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class DefaultHomeRepository @Inject constructor(
    private val api: HomeApi,
    private val mapper: HomeMapper,
    @param:DefaultDispatcher private val mappingDispatcher: CoroutineDispatcher,
) : HomeRepository {

    override suspend fun getBanners(): DataResult<List<HomeBanner>> = resultOf {
        val body = api.getBanners().requireResponse()
        withContext(mappingDispatcher) { body.data.mapNotNull(mapper::banner) }
    }

    override suspend fun getSections(page: Int): DataResult<HomeSectionsPage> = resultOf {
        val body = api.getSections(page = page, limit = SECTIONS_PER_PAGE, insideLimit = ITEMS_PER_SECTION).requireResponse()
        withContext(mappingDispatcher) { mapper.sectionsPage(body) }
    }

    override suspend fun getSectionItems(sectionId: String, page: Int): DataResult<SectionItemsPage> = resultOf {
        val body = api.getSectionItems(sectionId, page = page, insideLimit = ITEMS_PER_SECTION).requireResponse()
        withContext(mappingDispatcher) { mapper.sectionItemsPage(body) }
    }

    private companion object {
        const val STATUS_SUCCESS = "success"
        const val HTTP_OK = 200

        /** Unwraps the envelope; a "failed" envelope becomes an [ApiException]. */
        fun <T> ApiEnvelope<T>.requireResponse(): T {
            val failed = (status != null && status != STATUS_SUCCESS) || (statusCode != null && statusCode != HTTP_OK)
            if (failed || response == null) throw ApiException(statusCode ?: HTTP_OK, message)
            return response
        }
    }
}
