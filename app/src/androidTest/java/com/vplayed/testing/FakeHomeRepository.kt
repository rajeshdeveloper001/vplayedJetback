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
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Home data for UI tests: one banner and three pages of rows. [simulateOffline] makes the next
 * banner and row requests fail (counted separately, because the app requests them in parallel).
 */
@Singleton
class FakeHomeRepository @Inject constructor() : HomeRepository {

    private val bannerFailures = AtomicInteger(0)
    private val sectionFailures = AtomicInteger(0)
    val sectionCalls: MutableList<Int> = CopyOnWriteArrayList()

    /** The next [times] banner requests and [times] row requests fail as if offline. */
    fun simulateOffline(times: Int = 1) {
        bannerFailures.set(times)
        sectionFailures.set(times)
    }

    private fun AtomicInteger.consumeFailure(): Boolean = getAndUpdate { if (it > 0) it - 1 else 0 } > 0

    override suspend fun getBanners(): DataResult<List<HomeBanner>> =
        if (bannerFailures.consumeFailure()) DataResult.Failure(AppError.Network) else DataResult.Success(listOf(BANNER))

    override suspend fun getSections(page: Int): DataResult<HomeSectionsPage> {
        sectionCalls += page
        if (sectionFailures.consumeFailure()) return DataResult.Failure(AppError.Network)
        val sections = PAGES.getOrNull(page - 1) ?: return DataResult.Success(HomeSectionsPage(emptyList(), null, 0))
        return DataResult.Success(HomeSectionsPage(sections, nextPage = (page + 1).takeIf { it <= PAGES.size }, totalSections = 6))
    }

    override suspend fun getSectionItems(sectionId: String, page: Int): DataResult<SectionItemsPage> =
        DataResult.Success(SectionItemsPage(emptyList(), nextPage = null))

    companion object {
        const val HERO_TITLE = "The Last Kingdom"

        val BANNER = HomeBanner("banner-1", HERO_TITLE, "A kingdom divided.", ShowType.SERIES, null, null)

        private fun items(prefix: String, type: ContentType) = List(6) {
            ContentItem("$prefix-$it", "$prefix title ${it + 1}", null, type, genre = "Drama", duration = "1h 40m")
        }

        private fun row(id: String, title: String, style: SectionStyle, type: ContentType = ContentType.VIDEO) =
            HomeSection(id, title, type, style, 1, items(id, type), 6, null)

        val PAGES: List<List<HomeSection>> = listOf(
            listOf(row("trending", "Trending Now", SectionStyle.LANDSCAPE), row("live", "Live streaming", SectionStyle.LIVE, ContentType.LIVE)),
            listOf(row("top10", "Top 10 this week", SectionStyle.TOP_TEN), row("channels", "Popular Channels", SectionStyle.CHANNEL, ContentType.CHANNEL)),
            listOf(row("micro", "Micro Series", SectionStyle.PORTRAIT, ContentType.MICRO_SERIES), row("latest", "Latest Movies", SectionStyle.LANDSCAPE)),
        )
    }
}
