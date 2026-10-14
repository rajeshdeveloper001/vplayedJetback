package com.vplayed.feature.dashboard.home.domain

import com.vplayed.core.error.DataResult
import com.vplayed.feature.dashboard.home.domain.model.HomeBanner
import com.vplayed.feature.dashboard.home.domain.model.HomeSectionsPage
import com.vplayed.feature.dashboard.home.domain.model.SectionItemsPage

/** Home page data. Implemented over the REST API; faked in tests. */
interface HomeRepository {
    suspend fun getBanners(): DataResult<List<HomeBanner>>

    /** Rows for [page] (1-based). */
    suspend fun getSections(page: Int): DataResult<HomeSectionsPage>

    /** More items for one row. [page] is the row's `nextItemsPage`. */
    suspend fun getSectionItems(sectionId: String, page: Int): DataResult<SectionItemsPage>

    companion object {
        /** Rows per home page (`limit`). */
        const val SECTIONS_PER_PAGE = 3

        /** Items per row page (`inside_limit`). */
        const val ITEMS_PER_SECTION = 10
    }
}
