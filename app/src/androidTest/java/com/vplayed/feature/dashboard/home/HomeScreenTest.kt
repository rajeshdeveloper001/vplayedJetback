package com.vplayed.feature.dashboard.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vplayed.core.designsystem.theme.AppTheme
import com.vplayed.feature.dashboard.home.presentation.components.HomeTestTags
import com.vplayed.feature.dashboard.home.presentation.screen.HomeActions
import com.vplayed.feature.dashboard.home.presentation.screen.HomeScreen
import com.vplayed.feature.dashboard.home.presentation.viewmodel.HomeUiState
import com.vplayed.shared.core.error.AppError
import com.vplayed.shared.home.BundleAccent
import com.vplayed.shared.home.ContentItem
import com.vplayed.shared.home.ContentType
import com.vplayed.shared.home.HomeBanner
import com.vplayed.shared.home.HomeSection
import com.vplayed.shared.home.ItemDetails
import com.vplayed.shared.home.LiveStatus
import com.vplayed.shared.home.PagingStatus
import com.vplayed.shared.home.Price
import com.vplayed.shared.home.SectionStyle
import com.vplayed.shared.home.ShowType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Home screen states rendered from fixed UI state (no network, no ViewModel). */
@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val banner = HomeBanner("b1", "The Last Kingdom", "A kingdom divided.", ShowType.SERIES, null, null)

    private fun section(id: String, title: String, style: SectionStyle = SectionStyle.LANDSCAPE) = HomeSection(
        id = id,
        title = title,
        type = ContentType.VIDEO,
        style = style,
        rowCount = 1,
        items = List(4) { ContentItem("$id-$it", "$title ${it + 1}", null, ContentType.VIDEO, genre = "Drama", duration = "2h 00m") },
        totalItems = 4,
        nextItemsPage = null,
    )

    private val content = HomeUiState(
        isLoading = false,
        banners = listOf(banner),
        sections = listOf(section("trending", "Trending Now"), section("top", "Top 10", SectionStyle.TOP_TEN)),
        paging = PagingStatus.EndReached,
        nextPage = null,
    )

    private fun show(state: HomeUiState, actions: HomeActions = HomeActions()) {
        composeRule.setContent { AppTheme { HomeScreen(uiState = state, actions = actions) } }
    }

    @Test
    fun givenFirstLoad_whenShown_thenShimmerPlaceholderIsVisible() {
        composeRule.mainClock.autoAdvance = false // the shimmer animates forever
        show(HomeUiState())
        composeRule.mainClock.advanceTimeBy(FRAME_MILLIS)

        composeRule.onNodeWithTag(HomeTestTags.SHIMMER).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Loading").assertIsDisplayed()
    }

    @Test
    fun givenNothingLoaded_whenUserTapsRetry_thenRetryIsRequested() {
        var retries = 0
        show(HomeUiState(isLoading = false, error = AppError.Network), HomeActions(onRetry = { retries++ }))

        composeRule.onNodeWithText("You're offline. Check your connection and try again.").assertIsDisplayed()
        composeRule.onNodeWithText("Retry").performClick()

        assertEquals(1, retries)
    }

    @Test
    fun givenNoContent_whenShown_thenEmptyStateExplainsIt() {
        show(HomeUiState(isLoading = false, paging = PagingStatus.EndReached, nextPage = null))
        composeRule.onNodeWithText("Nothing to watch yet").assertIsDisplayed()
    }

    @Test
    fun givenContent_whenShown_thenHeroAndRowsFollowThePrototype() {
        show(content)

        composeRule.onNodeWithText("The Last Kingdom").assertIsDisplayed()
        // The chip names the content type, never a genre.
        composeRule.onNodeWithText("SERIES").assertIsDisplayed()
        composeRule.onNodeWithText("DRAMA").assertDoesNotExist()
        composeRule.onNodeWithText("Watch now").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Add to list").assertIsDisplayed()
        composeRule.onNodeWithTag(HomeTestTags.LIST).performScrollToNode(hasTestTag(HomeTestTags.section("top")))
        composeRule.onNodeWithContentDescription("1. Top 10 1").assertIsDisplayed()
    }

    @Test
    fun givenHero_whenUserTapsWatchNowAndAdd_thenBannerIsPassedToActions() {
        val watched = mutableListOf<String>()
        val added = mutableListOf<String>()
        show(content, HomeActions(onWatchNow = { watched += it.id }, onAddToList = { added += it.id }))

        composeRule.onNodeWithText("Watch now").performClick()
        composeRule.onNodeWithContentDescription("Add to list").performClick()

        assertEquals(listOf("b1"), watched)
        assertEquals(listOf("b1"), added)
    }

    @Test
    fun givenShortList_whenShown_thenNextPageIsRequestedAutomatically() {
        var requests = 0
        show(content.copy(paging = PagingStatus.Idle, nextPage = 2), HomeActions(onLoadNextPage = { requests++ }))

        composeRule.waitForIdle()
        assertTrue("Near the end, the next page loads without a tap", requests >= 1)
    }

    @Test
    fun givenNextPageFailed_whenUserTapsRetry_thenNextPageIsRequestedAgain() {
        var requests = 0
        show(content.copy(paging = PagingStatus.Failed(AppError.Server(500)), nextPage = 2), HomeActions(onLoadNextPage = { requests++ }))
        val before = requests

        composeRule.onNodeWithTag(HomeTestTags.LIST).performScrollToNode(hasTestTag(HomeTestTags.PAGING_RETRY))
        composeRule.onNodeWithText("Something went wrong on our side. Please try again.").assertIsDisplayed()
        composeRule.onNodeWithText("Retry").performClick()

        assertEquals(before + 1, requests)
    }

    @Test
    fun givenNextPageLoading_whenScrolledToEnd_thenRowShimmerIsShown() {
        composeRule.mainClock.autoAdvance = false
        show(content.copy(paging = PagingStatus.Loading, nextPage = 2))
        composeRule.mainClock.advanceTimeBy(FRAME_MILLIS)

        composeRule.onNodeWithTag(HomeTestTags.LIST).performScrollToNode(hasTestTag(HomeTestTags.PAGING_LOADING))
        composeRule.mainClock.advanceTimeBy(FRAME_MILLIS)
        composeRule.onNodeWithTag(HomeTestTags.PAGING_LOADING).assertIsDisplayed()
    }

    @Test
    fun givenTitleArtworkThatFailsToLoad_whenShown_thenTitleTextIsShownInstead() {
        val broken = banner.copy(titleImageUrl = "https://invalid.test/title.png")
        show(content.copy(banners = listOf(broken)))

        composeRule.waitUntil(LOAD_TIMEOUT_MILLIS) { composeRule.onAllNodesWithText("The Last Kingdom").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("The Last Kingdom").assertIsDisplayed()
    }

    @Test
    fun givenEveryRowType_whenScrolled_thenEachUsesItsPrototypeCard() {
        fun row(id: String, style: SectionStyle, type: ContentType, details: ItemDetails, title: String = id) = HomeSection(
            id = id, title = title, type = type, style = style, rowCount = 1,
            items = listOf(ContentItem("$id-1", "$id item", null, type, genre = "Drama", duration = "2m 28s", details = details)),
            totalItems = 1, nextItemsPage = null,
        )
        val rows = listOf(
            row("live", SectionStyle.LIVE, ContentType.LIVE, ItemDetails.Live(LiveStatus.LIVE, null, "Studio One")),
            row("soon", SectionStyle.LIVE, ContentType.LIVE, ItemDetails.Live(LiveStatus.UPCOMING, null, "Studio Two")),
            row("promo", SectionStyle.PROMO, ContentType.PROMO, ItemDetails.Promo(4_200_000, 312_000)),
            row("audio", SectionStyle.MUSIC, ContentType.AUDIO, ItemDetails.Audio("Sample Artist")),
            row("channel", SectionStyle.CHANNEL, ContentType.CHANNEL, ItemDetails.Channel(0xFFA0E3FF, "Films and originals", 1200)),
            row(
                "bundle", SectionStyle.OFFER, ContentType.BUNDLE,
                ItemDetails.Bundle("Bulk Purchase", "Three films", 7, Price(120.0, "₹", "INR"), Price(200.0, "₹", "INR"), 40, BundleAccent.BLUE),
            ),
        )
        show(content.copy(sections = rows))
        val list = composeRule.onNodeWithTag(HomeTestTags.LIST)

        list.performScrollToNode(hasTestTag(HomeTestTags.section("live")))
        composeRule.onNodeWithText("LIVE").assertIsDisplayed()
        composeRule.onNodeWithText("Watch Live").assertIsDisplayed()
        list.performScrollToNode(hasTestTag(HomeTestTags.section("soon")))
        composeRule.onNodeWithText("UPCOMING").assertIsDisplayed()
        composeRule.onNodeWithText("Set reminder").assertIsDisplayed()
        list.performScrollToNode(hasTestTag(HomeTestTags.section("promo")))
        composeRule.onNodeWithContentDescription("4.2M views, 312K likes").assertExists()
        list.performScrollToNode(hasTestTag(HomeTestTags.section("audio")))
        composeRule.onNodeWithText("Sample Artist").assertIsDisplayed()
        list.performScrollToNode(hasTestTag(HomeTestTags.section("channel")))
        composeRule.onNodeWithText("1.2K Followers").assertIsDisplayed()
        composeRule.onNodeWithText("Follow").assertIsDisplayed()
        list.performScrollToNode(hasTestTag(HomeTestTags.section("bundle")))
        composeRule.onNodeWithText("BULK PURCHASE").assertIsDisplayed()
        composeRule.onNodeWithText("Save up to 40%", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Explore bundles").assertIsDisplayed()
    }

    private companion object {
        const val FRAME_MILLIS = 100L
        const val LOAD_TIMEOUT_MILLIS = 10_000L
    }
}
