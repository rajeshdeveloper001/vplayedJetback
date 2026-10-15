package com.vplayed.feature.dashboard.home.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vplayed.R
import com.vplayed.core.designsystem.component.AppButton
import com.vplayed.core.designsystem.component.AppButtonStyle
import com.vplayed.core.designsystem.component.AppEmptyState
import com.vplayed.core.designsystem.component.AppErrorView
import com.vplayed.core.designsystem.dimension.WindowWidthClass
import com.vplayed.core.designsystem.dimension.currentWindowWidthClass
import com.vplayed.core.designsystem.theme.AppDesignSystem
import com.vplayed.core.designsystem.theme.AppTheme
import com.vplayed.core.ui.preview.ScreenPreviews
import com.vplayed.core.ui.userMessage
import com.vplayed.feature.dashboard.home.presentation.components.HeroBannerCarousel
import com.vplayed.feature.dashboard.home.presentation.components.HomeShimmer
import com.vplayed.feature.dashboard.home.presentation.components.HomeTestTags
import com.vplayed.feature.dashboard.home.presentation.components.HomeTopBar
import com.vplayed.feature.dashboard.home.presentation.components.RowShimmer
import com.vplayed.feature.dashboard.home.presentation.components.SectionRow
import com.vplayed.feature.dashboard.home.presentation.viewmodel.HomeUiState
import com.vplayed.feature.dashboard.home.presentation.viewmodel.HomeViewModel
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/** Start loading the next page of rows when this many rows remain below the viewport. */
private const val PAGE_PREFETCH_DISTANCE = 2

/** All user actions on Home. */
data class HomeActions(
    val onRetry: () -> Unit = {},
    val onRefresh: () -> Unit = {},
    val onLoadNextPage: () -> Unit = {},
    val onLoadMoreItems: (sectionId: String) -> Unit = {},
    val onOpenAppearance: () -> Unit = {},
    /** Opens the app's side menu (owned by the app shell). */
    val onOpenMenu: () -> Unit = {},
    /** Hero actions. Not wired yet: detail/player and watchlist screens are still to be built. */
    val onWatchNow: (HomeBanner) -> Unit = {},
    val onAddToList: (HomeBanner) -> Unit = {},
)

@Composable
fun HomeRoute(onOpenMenu: () -> Unit, onOpenAppearance: () -> Unit, viewModel: HomeViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnOpenMenu by rememberUpdatedState(onOpenMenu)
    val currentOnOpenAppearance by rememberUpdatedState(onOpenAppearance)
    HomeScreen(
        uiState = uiState,
        actions = HomeActions(
            onRetry = viewModel::retry,
            onRefresh = viewModel::refresh,
            onLoadNextPage = viewModel::loadNextPage,
            onLoadMoreItems = viewModel::loadMoreItems,
            onOpenAppearance = { currentOnOpenAppearance() },
            onOpenMenu = { currentOnOpenMenu() },
        ),
    )
}

/**
 * Home tab: hero banners and API-driven rows. Shows a shimmer on first load, a full-screen error
 * with Retry when nothing loaded, pull-to-refresh, and loads further rows while scrolling.
 */
@OptIn(ExperimentalMaterial3Api::class) // Top app bar scroll behavior and pull-to-refresh (Material3 1.4).
@Composable
fun HomeScreen(uiState: HomeUiState, actions: HomeActions, modifier: Modifier = Modifier) {
    val dimens = AppDesignSystem.dimens
    val compact = currentWindowWidthClass() == WindowWidthClass.COMPACT
    val cardWidth = if (compact) dimens.landscapeCardWidth else dimens.landscapeCardWidthLarge
    val posterWidth = if (compact) dimens.portraitCardWidth else dimens.portraitCardWidthLarge
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { HomeTopBar(onOpenMenu = actions.onOpenMenu, onOpenAppearance = actions.onOpenAppearance, scrollBehavior = scrollBehavior) },
        containerColor = AppDesignSystem.colors.background,
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { padding ->
        val error = uiState.error
        when {
            uiState.isLoading -> HomeShimmer(cardWidth = cardWidth, modifier = Modifier.padding(padding))
            error != null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                AppErrorView(message = error.userMessage(), onRetry = actions.onRetry)
            }
            uiState.isEmpty -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                AppEmptyState(
                    title = stringResource(R.string.home_empty_title),
                    message = stringResource(R.string.home_empty_message),
                )
            }
            else -> PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = actions.onRefresh,
                modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()),
            ) {
                HomeContent(
                    uiState = uiState,
                    actions = actions,
                    cardWidth = cardWidth,
                    posterWidth = posterWidth,
                    bottomPadding = padding.calculateBottomPadding(),
                )
            }
        }
    }
}

@Composable
private fun HomeContent(
    uiState: HomeUiState,
    actions: HomeActions,
    cardWidth: Dp,
    posterWidth: Dp,
    bottomPadding: Dp,
) {
    val dimens = AppDesignSystem.dimens
    val listState = rememberLazyListState()
    PagePagination(listState, actions.onLoadNextPage)
    LazyColumn(
            state = listState,
            contentPadding = PaddingValues(bottom = bottomPadding + dimens.spacing.xl),
            modifier = Modifier.fillMaxSize().testTag(HomeTestTags.LIST),
        ) {
            if (uiState.banners.isNotEmpty()) {
                item(key = "hero", contentType = "hero") {
                    HeroBannerCarousel(uiState.banners, onWatchNow = actions.onWatchNow, onAddToList = actions.onAddToList)
                }
            }
            items(uiState.sections, key = { it.id }, contentType = { it.style }) { section ->
                SectionRow(
                    section = section,
                    isLoadingMore = section.id in uiState.loadingSectionIds,
                    cardWidth = cardWidth,
                    posterWidth = posterWidth,
                    onLoadMore = actions.onLoadMoreItems,
                )
            }
            item(key = "paging", contentType = "paging") {
                PagingFooter(uiState.paging, cardWidth = cardWidth, onRetry = actions.onLoadNextPage)
            }
    }
}

@Composable
private fun PagingFooter(paging: PagingStatus, cardWidth: Dp, onRetry: () -> Unit) {
    val dimens = AppDesignSystem.dimens
    when (paging) {
        PagingStatus.Loading -> RowShimmer(cardWidth, Modifier.testTag(HomeTestTags.PAGING_LOADING))
        is PagingStatus.Failed -> Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimens.screenHorizontalPadding)
                .testTag(HomeTestTags.PAGING_RETRY),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(dimens.spacing.xs),
        ) {
            Text(
                text = paging.error.userMessage(),
                style = AppDesignSystem.typography.bodyMedium,
                color = AppDesignSystem.colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            AppButton(text = stringResource(R.string.action_retry), onClick = onRetry, style = AppButtonStyle.SECONDARY)
        }
        PagingStatus.Idle, PagingStatus.EndReached -> Unit
    }
}

/** Calls [onLoadNextPage] when the list nears its end; re-triggers after each page arrives. */
@Composable
private fun PagePagination(listState: LazyListState, onLoadNextPage: () -> Unit) {
    val currentOnLoadNextPage by rememberUpdatedState(onLoadNextPage)
    LaunchedEffect(listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            (lastVisible >= info.totalItemsCount - 1 - PAGE_PREFETCH_DISTANCE) to info.totalItemsCount
        }
            .distinctUntilChanged()
            .filter { (nearEnd, total) -> nearEnd && total > 0 }
            .collect { currentOnLoadNextPage() }
    }
}

// ---- Previews

private fun previewItems(prefix: String, count: Int, type: ContentType, details: (Int) -> ItemDetails = { ItemDetails.None }) = List(count) {
    ContentItem(
        id = "$prefix$it",
        title = "Title ${it + 1}",
        imageUrl = null,
        type = type,
        genre = "Action",
        duration = "1h 52m",
        details = details(it),
    )
}

private val previewState = HomeUiState(
    isLoading = false,
    banners = listOf(HomeBanner("b1", "Captain America: Brave New World", "Sam Wilson finds himself in the middle of an international incident.", ShowType.SERIES, null, null)),
    sections = listOf(
        HomeSection("s1", "Live streaming", ContentType.LIVE, SectionStyle.LIVE, 1, previewItems("l", 3, ContentType.LIVE) { ItemDetails.Live(if (it == 0) LiveStatus.LIVE else LiveStatus.UPCOMING, null, "VPlayed Originals") }, 3, null),
        HomeSection("s2", "Top 10 this week", ContentType.VIDEO, SectionStyle.TOP_TEN, 1, previewItems("t", 5, ContentType.VIDEO), 5, null),
        HomeSection("s3", "Videos / Series", ContentType.VIDEO, SectionStyle.LANDSCAPE, 1, previewItems("v", 5, ContentType.VIDEO), 5, null),
        HomeSection("s4", "Micro series", ContentType.MICRO_SERIES, SectionStyle.PORTRAIT, 1, previewItems("m", 5, ContentType.MICRO_SERIES), 5, null),
        HomeSection("s5", "Promos", ContentType.PROMO, SectionStyle.PROMO, 1, previewItems("p", 4, ContentType.PROMO) { ItemDetails.Promo(4_200_000, 312_000) }, 4, null),
        HomeSection("s6", "Audio", ContentType.AUDIO, SectionStyle.MUSIC, 1, previewItems("a", 4, ContentType.AUDIO) { ItemDetails.Audio("Hans Zimmer") }, 4, null),
        HomeSection("s7", "Popular channels", ContentType.CHANNEL, SectionStyle.CHANNEL, 1, previewItems("c", 3, ContentType.CHANNEL) { ItemDetails.Channel(0xFFA0E3FF, "Movies and originals", 1200) }, 3, null),
        HomeSection(
            "s8", "Bulk purchase", ContentType.BUNDLE, SectionStyle.OFFER, 1,
            previewItems("o", 3, ContentType.BUNDLE) {
                ItemDetails.Bundle("Bulk Purchase", "Curated packs of movies and shows for one price.", 7, Price(899.0, "₹", "INR"), Price(1499.0, "₹", "INR"), 40, BundleAccent.entries[it % BundleAccent.entries.size])
            },
            3, null,
        ),
    ),
    paging = PagingStatus.Loading,
)

@ScreenPreviews
@Composable
private fun HomeScreenPreview() {
    AppTheme { HomeScreen(uiState = previewState, actions = HomeActions()) }
}

@ScreenPreviews
@Composable
private fun HomeScreenLoadingPreview() {
    AppTheme { HomeScreen(uiState = HomeUiState(), actions = HomeActions()) }
}
