package com.vplayed.feature.dashboard.home.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import coil3.compose.AsyncImage
import com.vplayed.core.designsystem.component.cssLinearGradient
import com.vplayed.core.designsystem.component.mixWith
import com.vplayed.core.designsystem.theme.AppDesignSystem
import com.vplayed.shared.home.ContentItem
import com.vplayed.shared.home.ItemDetails
import com.vplayed.shared.l10n.Res
import com.vplayed.shared.l10n.home_follow
import com.vplayed.shared.l10n.home_followers
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

// Prototype `.chtile` (mirror of the design system's ChannelCard).
private const val GRADIENT_ANGLE = 155f
private const val BRAND_TOP = 0.55f
private const val BRAND_BOTTOM = 0.22f
private const val BRAND_RING = 0.26f
private const val DESCRIPTION_ALPHA = 0.72f
private const val PILL_ALPHA = 0.35f
private const val FOLLOW_FILL_ALPHA = 0.18f
private const val FOLLOW_RING_ALPHA = 0.45f
private const val LOGO_SHADOW_ALPHA = 0.25f
private val TilePadding = 20.dp
private val NameTop = 16.dp
private val LineTop = 6.dp
private val PillHeight = 26.dp
private val PillPaddingH = 12.dp
private val SpacerMin = 8.dp
private val LogoShadowBlur = 8.dp
private val LogoShadowY = 2.dp

/**
 * Channel tile (prototype `.chtile`): a 300dp plate tinted with the channel's brand color, white
 * logo well (profile picture, or initials in the brand color), name, description, follower pill
 * and a full-width glass "Follow" button pinned to the bottom.
 */
@Composable
internal fun ChannelTile(item: ContentItem, modifier: Modifier = Modifier, onFollow: () -> Unit = {}) {
    val channel = item.details as? ItemDetails.Channel
    val extended = AppDesignSystem.extendedColors
    val white = extended.onImageScrim
    val brand = channel?.brandColor?.let { Color(it) } ?: extended.textFaint
    val shape = AppDesignSystem.shapes.tile
    val ring = brand.mixWith(extended.hairline, BRAND_RING)
    Column(
        modifier = modifier
            .width(AppDesignSystem.dimens.channelTileWidth)
            .heightIn(min = AppDesignSystem.dimens.channelTileMinHeight)
            .clip(shape)
            .background(
                cssLinearGradient(
                    GRADIENT_ANGLE,
                    0f to brand.mixWith(extended.darkTileHigh, BRAND_TOP),
                    1f to brand.mixWith(extended.darkTileLow, BRAND_BOTTOM),
                ),
            )
            .border(AppDesignSystem.dimens.borderThin, ring, shape)
            .padding(TilePadding),
    ) {
        Column(Modifier.semantics(mergeDescendants = true) {}) {
            LogoWell(item, brand)
            Text(
                text = item.title,
                style = AppDesignSystem.textStyles.cardHeadline.copy(letterSpacing = 0.em),
                color = white,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = NameTop),
            )
            channel?.description?.let {
                Text(
                    text = it,
                    style = AppDesignSystem.textStyles.cardTitle.copy(fontWeight = FontWeight.Normal, lineHeight = DescriptionLineHeight),
                    color = white.copy(alpha = DESCRIPTION_ALPHA),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = LineTop),
                )
            }
            if (channel != null) {
                // A channel without a count shows 0, as before the shared layer.
                val followerCount = channel.followers ?: 0
                val followers = formatCompact(followerCount.toLong())
                Box(
                    modifier = Modifier
                        .padding(top = LineTop)
                        .height(PillHeight)
                        .background(AppDesignSystem.colors.scrim.copy(alpha = PILL_ALPHA), CircleShape)
                        .padding(horizontal = PillPaddingH),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = pluralStringResource(Res.plurals.home_followers, followerCount, followers),
                        style = AppDesignSystem.textStyles.cardSubtitle.copy(fontWeight = FontWeight.Medium),
                        color = white,
                        maxLines = 1,
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f).heightIn(min = SpacerMin))
        OutlinedButton(
            onClick = onFollow,
            shape = AppDesignSystem.shapes.button,
            border = BorderStroke(AppDesignSystem.dimens.borderThin, white.copy(alpha = FOLLOW_RING_ALPHA)),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = white.copy(alpha = FOLLOW_FILL_ALPHA), contentColor = white),
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.fillMaxWidth().height(AppDesignSystem.dimens.cardButtonHeight),
        ) {
            Text(stringResource(Res.string.home_follow), style = AppDesignSystem.textStyles.actionMedium)
        }
    }
}

/** `.chtile` description line height (1.35). */
private val DescriptionLineHeight = 1.35.em

/** `.clogo`: 60dp white well with a soft shadow; the channel picture, or initials in the brand color. */
@Composable
private fun LogoWell(item: ContentItem, brand: Color) {
    val shape = AppDesignSystem.shapes.panel
    val size = AppDesignSystem.dimens.channelLogoSize
    Box(
        modifier = Modifier
            .size(size)
            .dropShadow(shape, Shadow(radius = LogoShadowBlur, color = AppDesignSystem.colors.scrim.copy(alpha = LOGO_SHADOW_ALPHA), offset = DpOffset(0.dp, LogoShadowY)))
            .clip(shape)
            .background(AppDesignSystem.extendedColors.onImageScrim),
        contentAlignment = Alignment.Center,
    ) {
        Text(initials(item.title), style = AppDesignSystem.textStyles.monogram, color = brand, maxLines = 1)
        if (item.imageUrl != null) {
            AsyncImage(model = item.imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
    }
}
