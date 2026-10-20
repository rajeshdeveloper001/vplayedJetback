package com.vplayed.feature.dashboard.home.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vplayed.core.designsystem.component.ShimmerBox
import com.vplayed.core.designsystem.theme.AppDesignSystem
import com.vplayed.shared.l10n.Res
import com.vplayed.shared.l10n.common_loading
import org.jetbrains.compose.resources.stringResource

private val TitleSkeletonWidth = 140.dp
private val LineSkeletonHeight = 12.dp
private const val SKELETON_ROWS = 3
private const val SKELETON_CARDS = 4

/** First-load placeholder mirroring the home layout: hero, then rows of cards. */
@Composable
internal fun HomeShimmer(cardWidth: Dp, modifier: Modifier = Modifier) {
    val dimens = AppDesignSystem.dimens
    val loading = stringResource(Res.string.common_loading)
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag(HomeTestTags.SHIMMER)
            .semantics { contentDescription = loading },
        verticalArrangement = Arrangement.spacedBy(dimens.spacing.xl),
    ) {
        ShimmerBox(
            Modifier
                .fillMaxWidth()
                .height(dimens.heroMinHeight),
            shape = AppDesignSystem.shapes.extraSmall,
        )
        repeat(SKELETON_ROWS) { RowShimmer(cardWidth) }
    }
}

/** One placeholder row: title bar and a few cards. Also used while the next page loads. */
@Composable
internal fun RowShimmer(cardWidth: Dp, modifier: Modifier = Modifier) {
    val dimens = AppDesignSystem.dimens
    Column(
        modifier = modifier.padding(start = dimens.screenHorizontalPadding),
        verticalArrangement = Arrangement.spacedBy(dimens.spacing.sm),
    ) {
        ShimmerBox(Modifier.width(TitleSkeletonWidth).height(dimens.spacing.lg))
        Row(horizontalArrangement = Arrangement.spacedBy(dimens.cardSpacing)) {
            repeat(SKELETON_CARDS) { CardShimmer(cardWidth) }
        }
    }
}

/** Placeholder for one 16:9 card (or 2:3 poster) with two text lines. */
@Composable
internal fun CardShimmer(width: Dp, modifier: Modifier = Modifier, portrait: Boolean = false) {
    val dimens = AppDesignSystem.dimens
    Column(modifier.width(width), verticalArrangement = Arrangement.spacedBy(dimens.spacing.xs)) {
        ShimmerBox(
            Modifier.fillMaxWidth().aspectRatio(if (portrait) POSTER_ASPECT_RATIO else CARD_ASPECT_RATIO),
            shape = AppDesignSystem.shapes.card,
        )
        ShimmerBox(Modifier.fillMaxWidth(fraction = 0.8f).height(LineSkeletonHeight))
        ShimmerBox(Modifier.fillMaxWidth(fraction = 0.5f).height(LineSkeletonHeight))
    }
}
