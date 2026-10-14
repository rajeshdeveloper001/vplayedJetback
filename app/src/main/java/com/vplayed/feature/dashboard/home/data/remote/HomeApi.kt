package com.vplayed.feature.dashboard.home.data.remote

import com.vplayed.feature.dashboard.home.data.remote.dto.ApiEnvelope
import com.vplayed.feature.dashboard.home.data.remote.dto.BannerListDto
import com.vplayed.feature.dashboard.home.data.remote.dto.HomePageDto
import com.vplayed.feature.dashboard.home.data.remote.dto.SectionItemsPageDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface HomeApi {

    /** Hero carousel content. */
    @GET("mediaview/api/v1/homebanners")
    suspend fun getBanners(): ApiEnvelope<BannerListDto>

    /** Home rows, paginated by section: [limit] sections per page, [insideLimit] items per section. */
    @GET("mediaview/api/v1/home")
    suspend fun getSections(
        @Query("page") page: Int,
        @Query("limit") limit: Int,
        @Query("inside_limit") insideLimit: Int,
    ): ApiEnvelope<HomePageDto>

    /** Further items of one row (horizontal pagination). */
    @GET("mediaview/api/v1/home/all/{sectionId}")
    suspend fun getSectionItems(
        @Path("sectionId") sectionId: String,
        @Query("page") page: Int,
        @Query("inside_limit") insideLimit: Int,
    ): ApiEnvelope<SectionItemsPageDto>
}
