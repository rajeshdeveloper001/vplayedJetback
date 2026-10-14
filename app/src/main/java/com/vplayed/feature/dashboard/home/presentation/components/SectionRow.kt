package com.vplayed.feature.dashboard.home.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.vplayed.R
import com.vplayed.core.designsystem.dimension.WindowWidthClass
import com.vplayed.core.designsystem.dimension.currentWindowWidthClass
import com.vplayed.core.designsystem.icon.AppIcons
import com.vplayed.core.designsystem.theme.AppDesignSystem
import com.vplayed.feature.dashboard.home.domain.model.ContentItem
import com.vplayed.feature.dashboard.home.domain.model.HomeSection
import com.vplayed.feature.dashboard.home.domain.model.SectionStyle
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/** Start loading the next items when this many remain unseen. */
private const val ROW_PREFETCH_DISTANCE = 3

// Prototype `#rows > .row`: 22dp above, 4dp below; `.row-head` 44dp tall, 12dp side padding.
private val RowTop = 22.dp
private val RowBottom = 4.dp
private val HeadStart = 12.dp
/** The 44dp `.row-va-arrow` sits 3dp from the edge (its 28dp chevron 11dp further in). */
private val HeadEnd = 3.dp
private val ChevronSize = 28.dp

/** `.row-head h2`: clamp(18px, 5vw, 22px), -0.01em, on phones. */
private const val TITLE_VW = 0.05f
private const val TITLE_MIN = 18f
private const val TITLE_MAX = 22f

// `.premium` section: 20dp margins around a track padded 10/20 (card at x=20, 14dp apart).
private val OfferTop = 30.dp
private val OfferBottom = 32.dp
private val OfferGap = 14.dp
private val OfferSide = 20.dp

/** Rail paddings per row type (prototype `.rail`, `.rail.top10`, `.live-rail`; margins folded in). */
private data class RailSpec(val top: Dp, val bottom: Dp, val gap: Dp)

private val DefaultRail = RailSpec(top = 14.dp, bottom = 18.dp, gap = 8.dp)
private val TopTenRail = RailSpec(top = 28.dp, bottom = 38.dp, gap = 4.dp)
private val LiveRail = RailSpec(top = 16.dp, bottom = 20.dp, gap = 8.dp)
private val RailSide = 16.dp

/**
 * One home row in the prototype layout: a 44dp header (title and "view all" chevron) and a
 * horizontal rail of the row's card type. Bulk-purchase rows have no header. Two-row sections show
 * stacked pairs that scroll together. Requests more items via [onLoadMore] near the end.
 */
@Composable
internal fun SectionRow(
    section: HomeSection,
    isLoadingMore: Boolean,
    cardWidth: Dp,
    posterWidth: Dp,
    onLoadMore: (sectionId: String) -> Unit,
    modifier: Modifier = Modifier,
    onViewAll: (HomeSection) -> Unit = {},
    onItemClick: (ContentItem) -> Unit = {},
) {
    val listState = rememberLazyListState()
    RowPagination(listState = listState, section = section, onLoadMore = onLoadMore)
    val offer = section.style == SectionStyle.OFFER
    val rail = when (section.style) {
        SectionStyle.TOP_TEN -> TopTenRail
        SectionStyle.LIVE -> LiveRail
        else -> DefaultRail
    }
    Column(
        modifier
            .fillMaxWidth()
            .testTag(HomeTestTags.section(section.id))
            .padding(top = if (offer) OfferTop else RowTop, bottom = if (offer) OfferBottom else RowBottom),
    ) {
        if (!offer && section.title.isNotBlank()) RowHeader(section, onViewAll)
        val columns = if (section.rowCount > 1) section.items.chunked(section.rowCount) else section.items.map(::listOf)
        val offerWidth = offerCardWidth()
        // The top-10 title hangs below its card into the rail padding (prototype absolute `.t10ttl`).
        val overhang = if (section.style == SectionStyle.TOP_TEN) TopTenTitleOverhang else 0.dp
        LazyRow(
            state = listState,
            contentPadding = if (offer) {
                PaddingValues(horizontal = OfferSide)
            } else {
                PaddingValues(start = RailSide, end = RailSide, top = rail.top, bottom = (rail.bottom - overhang).coerceAtLeast(0.dp))
            },
            horizontalArrangement = Arrangement.spacedBy(if (offer) OfferGap else rail.gap),
            verticalAlignment = Alignment.Top,
            modifier = Modifier
                .testTag(HomeTestTags.row(section.id))
                .trimBottom(overhang - rail.bottom),
        ) {
            itemsIndexed(columns, key = { _, column -> column.first().id }, contentType = { _, _ -> section.style }) { index, column ->
                Column(verticalArrangement = Arrangement.spacedBy(rail.gap)) {
                    column.forEachIndexed { offset, item ->
                        SectionItem(
                            style = section.style,
                            item = item,
                            rank = index * section.rowCount + offset + 1,
                            cardWidth = cardWidth,
                            posterWidth = posterWidth,
                            offerWidth = offerWidth,
                            onClick = { onItemClick(item) },
                        )
                    }
                }
            }
            if (isLoadingMore) {
                item(key = "loading", contentType = "loading") {
                    val portrait = section.style == SectionStyle.PORTRAIT || section.style == SectionStyle.TOP_TEN
                    CardShimmer(if (portrait) posterWidth else cardWidth, portrait = portrait)
                }
            }
        }
    }
}

/** `.row-head`: bold title scaled with the window (phones) and a chevron that opens the full list. */
@Composable
private fun RowHeader(section: HomeSection, onViewAll: (HomeSection) -> Unit) {
    val dimens = AppDesignSystem.dimens
    val header = AppDesignSystem.textStyles.header
    val titleStyle = if (currentWindowWidthClass() == WindowWidthClass.COMPACT) {
        val widthDp = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp().value }
        val target = (widthDp * TITLE_VW).coerceIn(TITLE_MIN, TITLE_MAX)
        header.copy(fontSize = header.fontSize * (target / header.fontSize.value), letterSpacing = (-0.01).em)
    } else {
        header
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimens.rowHeaderHeight)
            .padding(start = HeadStart, end = HeadEnd),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = section.title,
            style = titleStyle,
            color = AppDesignSystem.colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        if (section.hasViewAll) {
            IconButton(onClick = { onViewAll(section) }, modifier = Modifier.size(dimens.rowHeaderHeight)) {
                Icon(
                    painterResource(AppIcons.ChevronRight),
                    contentDescription = stringResource(R.string.home_view_all, section.title),
                    tint = AppDesignSystem.colors.onSurface,
                    modifier = Modifier.size(ChevronSize),
                )
            }
        }
    }
}

@Composable
private fun SectionItem(
    style: SectionStyle,
    item: ContentItem,
    rank: Int,
    cardWidth: Dp,
    posterWidth: Dp,
    offerWidth: Dp,
    onClick: () -> Unit,
) {
    when (style) {
        SectionStyle.LANDSCAPE -> LandscapeCard(item, width = cardWidth)
        SectionStyle.PORTRAIT -> PortraitCard(item, width = posterWidth)
        SectionStyle.TOP_TEN -> TopTenCard(item, rank = rank)
        SectionStyle.LIVE -> LiveCard(item, onClick = onClick)
        SectionStyle.PROMO -> PromoCard(item, width = cardWidth)
        SectionStyle.MUSIC -> MusicCard(item)
        SectionStyle.CHANNEL -> ChannelTile(item, onFollow = onClick)
        SectionStyle.OFFER -> OfferCard(item, width = offerWidth, onClick = onClick)
    }
}

/** `.premium-in` is the window width minus 72dp on phones; capped on wide windows. */
@Composable
private fun offerCardWidth(): Dp {
    val dimens = AppDesignSystem.dimens
    val windowWidth = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() }
    return (windowWidth - dimens.offerCardInset).coerceAtMost(dimens.offerCardMaxWidth)
}

/** Reports a shorter height to the parent (content may draw into the following space). */
private fun Modifier.trimBottom(amount: Dp): Modifier = if (amount <= 0.dp) {
    this
} else {
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val trim = amount.roundToPx().coerceAtMost(placeable.height)
        layout(placeable.width, placeable.height - trim) { placeable.place(0, 0) }
    }
}

/** Calls [onLoadMore] when the row is scrolled near its end and more items exist. */
@Composable
private fun RowPagination(listState: LazyListState, section: HomeSection, onLoadMore: (String) -> Unit) {
    val currentSection by rememberUpdatedState(section)
    val currentOnLoadMore by rememberUpdatedState(onLoadMore)
    LaunchedEffect(listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            // Pair with the count so a still-near-end row re-triggers after new items arrive.
            (lastVisible >= info.totalItemsCount - ROW_PREFETCH_DISTANCE) to info.totalItemsCount
        }
            .distinctUntilChanged()
            .filter { (nearEnd, total) -> nearEnd && total > 0 }
            .collect { if (currentSection.canLoadMoreItems) currentOnLoadMore(currentSection.id) }
    }
}
