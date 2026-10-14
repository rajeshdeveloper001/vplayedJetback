package com.vplayed.feature.dashboard.home

import com.vplayed.feature.dashboard.home.data.mapper.HomeMapper
import com.vplayed.feature.dashboard.home.data.remote.dto.ApiEnvelope
import com.vplayed.feature.dashboard.home.data.remote.dto.BannerListDto
import com.vplayed.feature.dashboard.home.data.remote.dto.HomePageDto
import com.vplayed.feature.dashboard.home.data.remote.dto.SectionItemsPageDto
import com.vplayed.feature.dashboard.home.domain.model.BundleAccent
import com.vplayed.feature.dashboard.home.domain.model.ContentType
import com.vplayed.feature.dashboard.home.domain.model.ItemDetails
import com.vplayed.feature.dashboard.home.domain.model.LiveStatus
import com.vplayed.feature.dashboard.home.domain.model.SectionStyle
import com.vplayed.feature.dashboard.home.domain.model.ShowType
import com.vplayed.testing.Fixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeMapperTest {

    private val mapper = HomeMapper(imageBaseUrl = "https://images.test/")

    private fun page(name: String) =
        mapper.sectionsPage(Fixtures.json.decodeFromString<ApiEnvelope<HomePageDto>>(Fixtures.read(name)).response!!)

    private val allSections by lazy {
        (page("home/home_page1.json").sections + page("home/home_page2.json").sections + page("home/home_page3.json").sections)
            .associateBy { it.id }
    }

    private val banners = Fixtures.json.decodeFromString<ApiEnvelope<BannerListDto>>(Fixtures.read("home/banners.json"))
        .response!!.data.mapNotNull(mapper::banner)

    @Test
    fun givenBannersWithAndWithoutContent_whenMapping_thenOnlyBannersWithTitlesRemain() {
        assertEquals(listOf("Sample Feature", "Second Feature"), banners.map { it.title })
    }

    @Test
    fun givenShowType_whenMappingBanner_thenChipUsesContentTypeNotGenre() {
        assertEquals(ShowType.SERIES, banners[0].showType)
        assertEquals(ShowType.MICRO_SERIES, banners[1].showType)
    }

    @Test
    fun givenBannerImages_whenMapping_thenPrefersMobileBannerAndPngTitleArtwork() {
        assertEquals("https://images.test/banners/feature-mobile.jpg", banners[0].imageUrl)
        assertEquals("https://images.test/titles/feature-mobile.png", banners[0].titleImageUrl)
        assertEquals("https://images.test/thumbs/second-tl.jpg", banners[1].imageUrl)
        assertNull("No title artwork: the hero shows the title text", banners[1].titleImageUrl)
    }

    @Test
    fun givenSectionWithoutItems_whenMapping_thenItIsDropped() {
        val result = page("home/home_page1.json")
        assertEquals(listOf("sec-live", "sec-videos"), result.sections.map { it.id })
        assertEquals(2, result.nextPage)
    }

    @Test
    fun givenSectionTypes_whenMapping_thenEachGetsItsPrototypeComponent() {
        val styles = allSections.mapValues { it.value.style }
        assertEquals(SectionStyle.LIVE, styles["sec-live"])
        assertEquals(SectionStyle.LANDSCAPE, styles["sec-videos"])
        assertEquals(SectionStyle.TOP_TEN, styles["sec-top"])
        assertEquals(SectionStyle.CHANNEL, styles["sec-channels"])
        assertEquals(SectionStyle.OFFER, styles["sec-bundles"])
        assertEquals(SectionStyle.PORTRAIT, styles["sec-micro"])
        assertEquals(SectionStyle.PROMO, styles["sec-promos"])
        assertEquals(SectionStyle.MUSIC, styles["sec-audio"])
    }

    @Test
    fun givenVideo_whenMapping_thenUsesMidSizeThumbnailGenreTitleAndDuration() {
        val video = allSections.getValue("sec-videos").items[0]
        assertEquals("https://images.test/thumbs/vid-1-tc.jpg", video.imageUrl)
        assertEquals("Slug default_genre resolves to its display title", "Thriller", video.genre)
        assertEquals("1h 30m", video.duration)
        assertEquals(ShowType.VIDEO, video.showType)
        assertEquals(ContentType.VIDEO, video.type)
    }

    @Test
    fun givenNumericDurationAndAbsoluteImage_whenMapping_thenBothAreHandled() {
        val video = allSections.getValue("sec-videos").items[1]
        assertEquals("2m 05s", video.duration)
        assertNull(video.genre)
        assertEquals("https://cdn.example.test/absolute/vid-2.jpg", video.imageUrl)
    }

    @Test
    fun givenSeveralPrices_whenMapping_thenPrefersRupees() {
        val price = allSections.getValue("sec-videos").items[0].price!!
        assertEquals("₹", price.currencySign)
        assertEquals(49.0, price.amount, 0.0)
    }

    @Test
    fun givenStreamingStatus_whenMappingLiveItems_thenLiveAndUpcomingCarryStartTimeAndChannel() {
        val live = allSections.getValue("sec-live").items
        val now = live[0].details as ItemDetails.Live
        val later = live[1].details as ItemDetails.Live
        assertTrue(live[0].isLive)
        assertEquals(LiveStatus.LIVE, now.status)
        assertEquals(1_791_816_000_000L, now.startsAtMillis) // 2026-10-12T14:40:00Z
        assertEquals("Sample Studio", now.channelName)
        assertFalse(live[1].isLive)
        assertEquals(LiveStatus.UPCOMING, later.status)
        assertEquals(1_791_905_400_000L, later.startsAtMillis) // 2026-10-13T15:30:00Z, no millis
        assertNull(live[1].imageUrl)
    }

    @Test
    fun givenTopTenAndMicroSeries_whenMapping_thenUsePortraitThumbnailAndEpisodes() {
        assertEquals("https://images.test/posters/top-1.jpg", allSections.getValue("sec-top").items[0].imageUrl)
        val micro = allSections.getValue("sec-micro")
        assertEquals("https://images.test/posters/micro-1.jpg", micro.items[0].imageUrl)
        assertEquals(12, micro.items[0].episodeCount)
        assertEquals("Lowercase default_genre title resolves to the genre list title", "Drama", micro.items[0].genre)
        assertEquals(2, micro.rowCount)
    }

    @Test
    fun givenChannel_whenMapping_thenBrandColorDescriptionAndFollowers() {
        val channel = allSections.getValue("sec-channels").items[0]
        assertEquals("Sample Channel", channel.title)
        assertEquals("https://images.test/images/channel.png", channel.imageUrl)
        assertEquals(ItemDetails.Channel(brandColor = 0xFFA0E3FF, description = "Films and originals", followers = 1200), channel.details)
    }

    @Test
    fun givenDiscountedBundle_whenMapping_thenPriceIsReducedAndOriginalIsKept() {
        val bundle = allSections.getValue("sec-bundles").items[0]
        val details = bundle.details as ItemDetails.Bundle
        assertEquals("Weekend Pack", bundle.title)
        assertEquals("Special for you", details.label)
        assertEquals("Three films for one price.", details.description)
        assertEquals(120.0, details.price!!.amount, 0.0)
        assertEquals(200.0, details.originalPrice!!.amount, 0.0)
        assertEquals(40, details.discountPercent)
        assertEquals(BundleAccent.BLUE, details.accent)
    }

    @Test
    fun givenBundleWithoutDiscount_whenMapping_thenFullPriceValidityAndGoldAccent() {
        val details = allSections.getValue("sec-bundles").items[1].details as ItemDetails.Bundle
        assertEquals(99.0, details.price!!.amount, 0.0)
        assertNull("discounted_price 0 means no discount, whatever the percentage says", details.originalPrice)
        assertNull(details.discountPercent)
        assertEquals(30, details.days)
        assertEquals(BundleAccent.GOLD, details.accent)
    }

    @Test
    fun givenPromoAndAudio_whenMapping_thenStatsDurationAndArtist() {
        val promo = allSections.getValue("sec-promos").items[0]
        assertEquals(ItemDetails.Promo(views = 4_200_000, likes = 312_000), promo.details)
        assertEquals("2m 28s", promo.duration)
        val audio = allSections.getValue("sec-audio").items[0]
        assertEquals(ItemDetails.Audio(artist = "Sample Artist"), audio.details)
        assertEquals("2h 38m", audio.duration)
    }

    @Test
    fun givenRowEndpointWithLocalizedTitle_whenMappingItems_thenReturnsItemsAndNextPage() {
        val dto = Fixtures.json.decodeFromString<ApiEnvelope<SectionItemsPageDto>>(Fixtures.read("home/section_items_page2.json"))
        val result = mapper.sectionItemsPage(dto.response!!)
        assertEquals(listOf("vid-11"), result.items.map { it.id })
        assertEquals(3, result.nextPage)
        assertEquals("Drama", result.items[0].genre)
    }
}
