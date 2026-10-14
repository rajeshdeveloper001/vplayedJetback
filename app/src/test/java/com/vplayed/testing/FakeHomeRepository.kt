package com.vplayed.testing

import com.vplayed.core.error.AppError
import com.vplayed.core.error.DataResult
import com.vplayed.feature.dashboard.home.domain.HomeRepository
import com.vplayed.feature.dashboard.home.domain.model.ContentItem
import com.vplayed.feature.dashboard.home.domain.model.ContentType
import com.vplayed.feature.dashboard.home.domain.model.HomeBanner
import com.vplayed.feature.dashboard.home.domain.model.HomeSection
import com.vplayed.feature.dashboard.home.domain.model.HomeSectionsPage
import com.vplayed.feature.dashboard.home.domain.model.SectionItemsPage
import com.vplayed.feature.dashboard.home.domain.model.SectionStyle
import com.vplayed.feature.dashboard.home.domain.model.ShowType
import kotlinx.coroutines.CompletableDeferred

/**
 * Scriptable [HomeRepository]. Each call records its arguments; results come from the maps, and
 * a page can be held with [holdSections] to observe in-flight states.
 */
class FakeHomeRepository : HomeRepository {
    var banners: DataResult<List<HomeBanner>> = DataResult.Success(listOf(banner("b1")))
    val sectionPages = mutableMapOf<Int, DataResult<HomeSectionsPage>>()
    val itemPages = mutableMapOf<Pair<String, Int>, DataResult<SectionItemsPage>>()
    val sectionCalls = mutableListOf<Int>()
    val itemCalls = mutableListOf<Pair<String, Int>>()
    private val holds = mutableMapOf<Int, CompletableDeferred<Unit>>()

    fun holdSections(page: Int): CompletableDeferred<Unit> = CompletableDeferred<Unit>().also { holds[page] = it }

    override suspend fun getBanners() = banners

    override suspend fun getSections(page: Int): DataResult<HomeSectionsPage> {
        sectionCalls += page
        holds.remove(page)?.await()
        return sectionPages[page] ?: DataResult.Failure(AppError.Server(404))
    }

    override suspend fun getSectionItems(sectionId: String, page: Int): DataResult<SectionItemsPage> {
        itemCalls += sectionId to page
        return itemPages[sectionId to page] ?: DataResult.Failure(AppError.Server(404))
    }

    companion object {
        fun banner(id: String) = HomeBanner(id, "Banner $id", null, ShowType.VIDEO, null, null)

        fun item(id: String) = ContentItem(id, "Title $id", null, ContentType.VIDEO, genre = "Drama")

        fun section(id: String, items: Int = 3, nextItemsPage: Int? = null) = HomeSection(
            id = id,
            title = "Row $id",
            type = ContentType.VIDEO,
            style = SectionStyle.LANDSCAPE,
            rowCount = 1,
            items = List(items) { item("$id-$it") },
            totalItems = items,
            nextItemsPage = nextItemsPage,
        )

        fun page(vararg sections: HomeSection, next: Int?) = DataResult.Success(
            HomeSectionsPage(sections.toList(), nextPage = next, totalSections = sections.size),
        )
    }
}
