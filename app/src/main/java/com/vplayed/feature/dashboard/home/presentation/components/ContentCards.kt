package com.vplayed.feature.dashboard.home.presentation.components

import android.graphics.Paint
import android.graphics.Typeface
import android.icu.text.CompactDecimalFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import coil3.compose.AsyncImage
import com.vplayed.R
import com.vplayed.core.designsystem.component.skeleton
import com.vplayed.core.designsystem.icon.AppIcons
import com.vplayed.core.designsystem.theme.AppDesignSystem
import com.vplayed.shared.home.ContentItem
import com.vplayed.shared.home.ItemDetails
import java.util.Locale

/** Landscape artwork is 16:9 (`.hshot`); posters are 221:295 (`.pcard .poster`) and 3:4 (`.t10 .poster`). */
internal const val CARD_ASPECT_RATIO = 16f / 9f
internal const val POSTER_ASPECT_RATIO = 221f / 295f
internal const val TOP_TEN_POSTER_ASPECT_RATIO = 3f / 4f

/** `.hshot .fade`: black 75% at the bottom (2%) to transparent at 48%. */
private const val CARD_FADE_ALPHA = 0.75f
private const val CARD_FADE_START = 0.52f
private const val CARD_FADE_END = 0.98f

/** `.pdur` duration pill: neutral-950 at 72%. */
private const val PILL_ALPHA = 0.72f

/** Card meta separator `.ssep`: "·" at 1.3em, semibold. */
private const val META_SEPARATOR = "·"
private const val META_SEPARATOR_SCALE = 1.3f

/** Top-10 numeral (SVG viewBox 150×210 / 220×210, text at y=204, font 280, scaled into a 168dp box). */
private const val RANK_VIEWBOX_HEIGHT = 210f
private const val RANK_BASELINE = 204f
private const val RANK_FONT_SIZE = 280f
private const val RANK_SINGLE_VIEWBOX_WIDTH = 150f
private const val RANK_DOUBLE_VIEWBOX_WIDTH = 220f
private const val RANK_TRACKING_SINGLE = -0.06f
private const val RANK_TRACKING_DOUBLE = -0.11f
private const val RANK_STROKE_DARK = 1.6f
private const val RANK_STROKE_LIGHT = 2.5f
private const val RANK_OUTLINE_ALPHA_DARK = 0.15f
private const val RANK_OUTLINE_ALPHA_LIGHT = 0.28f

/** `.t10num.t10num-2`: two-digit numerals overlap the poster further. */
private val TopTenDoubleOverlap = 58.dp

/** `.t10 .t10ttl`: title under the poster, 10dp below the card, indented to the poster's text column. */
private val TopTenTitleTop = 10.dp
private val TopTenTitleStart = 102.dp

/** Space the top-10 title takes below the card (two 17.5sp lines + gap). */
internal val TopTenTitleOverhang = 45.dp

/**
 * Network image on the prototype's card fill (neutral-800) with its 1dp hairline drawn over the
 * artwork. Shows the skeleton while loading and when the URL is missing or broken.
 */
@Composable
internal fun ContentImage(
    url: String?,
    shape: Shape,
    modifier: Modifier = Modifier,
    bordered: Boolean = true,
    contentScale: ContentScale = ContentScale.Crop,
    overlay: @Composable BoxScope.() -> Unit = {},
) {
    val border = if (bordered) {
        Modifier.border(AppDesignSystem.dimens.borderThin, AppDesignSystem.extendedColors.hairline, shape)
    } else {
        Modifier
    }
    Box(modifier.clip(shape).background(AppDesignSystem.colors.surfaceContainer).skeleton(shape).then(border)) {
        if (url != null) {
            AsyncImage(model = url, contentDescription = null, contentScale = contentScale, modifier = Modifier.fillMaxSize())
        }
        overlay()
    }
}

/** `.fade` gradient at the bottom of 16:9 artwork. */
@Composable
private fun CardFade() {
    val scrim = AppDesignSystem.colors.scrim
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    CARD_FADE_START to Color.Transparent,
                    CARD_FADE_END to scrim.copy(alpha = CARD_FADE_ALPHA),
                ),
            ),
    )
}

/** "Action · 2h 08m" with the prototype's larger separator dot. */
@Composable
internal fun MetaText(parts: List<String>, color: Color, maxLines: Int = 1, style: TextStyle = AppDesignSystem.textStyles.cardSubtitle) {
    if (parts.isEmpty()) return
    val text = buildAnnotatedString {
        parts.forEachIndexed { index, part ->
            if (index > 0) {
                append(" ")
                withStyle(SpanStyle(fontSize = META_SEPARATOR_SCALE.em, fontWeight = FontWeight.SemiBold)) { append(META_SEPARATOR) }
                append(" ")
            }
            append(part)
        }
    }
    Text(text = text, style = style, color = color, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
}

/** The `.ssep` dot on its own (between icon groups). */
@Composable
private fun MetaSeparator(color: Color) {
    Text(
        text = META_SEPARATOR,
        style = AppDesignSystem.textStyles.cardSubtitle.copy(fontSize = AppDesignSystem.textStyles.cardSubtitle.fontSize * META_SEPARATOR_SCALE, fontWeight = FontWeight.SemiBold),
        color = color,
        modifier = Modifier.padding(horizontal = StatGap),
    )
}

/** Genre, then running time — or episode count for series and micro series (prototype `metaCount`). */
@Composable
internal fun ContentItem.metaParts(): List<String> {
    val length = duration ?: episodeCount?.let { pluralStringResource(R.plurals.home_episodes, it, it) }
    return listOfNotNull(genre, length)
}

/** `.hinfo`: title (2 lines) and meta, 10dp under the artwork, inset 2dp. */
@Composable
private fun CardInfo(title: String, meta: @Composable () -> Unit) {
    Column(Modifier.padding(top = CardInfoTop, start = CardInfoInset, end = CardInfoInset)) {
        Text(
            text = title,
            style = AppDesignSystem.textStyles.cardTitle,
            color = AppDesignSystem.colors.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(CardMetaTop))
        meta()
    }
}

private val CardInfoTop = 10.dp
private val CardInfoInset = 2.dp
private val CardMetaTop = 3.dp

/** Standard video / series card (prototype `.hcard`): 16:9 artwork with fade, title, "Genre · duration". */
@Composable
internal fun LandscapeCard(item: ContentItem, width: Dp, modifier: Modifier = Modifier) {
    Column(modifier.width(width).semantics(mergeDescendants = true) {}) {
        ContentImage(
            url = item.imageUrl,
            shape = AppDesignSystem.shapes.card,
            modifier = Modifier.fillMaxWidth().aspectRatio(CARD_ASPECT_RATIO),
            overlay = { CardFade() },
        )
        CardInfo(item.title) { MetaText(item.metaParts(), color = AppDesignSystem.colors.onSurfaceVariant) }
    }
}

/** Micro-series card (prototype `.pcard`): 221:295 poster, title and meta in soft grey (may wrap). */
@Composable
internal fun PortraitCard(item: ContentItem, width: Dp, modifier: Modifier = Modifier) {
    Column(modifier.width(width).semantics(mergeDescendants = true) {}) {
        ContentImage(
            url = item.imageUrl,
            shape = AppDesignSystem.shapes.card,
            modifier = Modifier.fillMaxWidth().aspectRatio(POSTER_ASPECT_RATIO),
        )
        CardInfo(item.title) { MetaText(item.metaParts(), color = AppDesignSystem.extendedColors.textSoft, maxLines = 2) }
    }
}

/** Promo card (prototype `.hcard.pstat`): 16:9 artwork with a duration pill, title, views · likes. */
@Composable
internal fun PromoCard(item: ContentItem, width: Dp, modifier: Modifier = Modifier) {
    val stats = item.details as? ItemDetails.Promo
    val views = formatCompact(stats?.views ?: 0)
    val likes = formatCompact(stats?.likes ?: 0)
    val statsDescription = stringResource(R.string.home_promo_stats, views, likes)
    Column(modifier.width(width).semantics(mergeDescendants = true) {}) {
        ContentImage(
            url = item.imageUrl,
            shape = AppDesignSystem.shapes.card,
            modifier = Modifier.fillMaxWidth().aspectRatio(CARD_ASPECT_RATIO),
            overlay = {
                CardFade()
                item.duration?.let { DurationPill(it, Modifier.align(Alignment.BottomEnd).padding(PillInset)) }
            },
        )
        CardInfo(item.title) {
            val color = AppDesignSystem.colors.onSurfaceVariant
            val style = AppDesignSystem.textStyles.cardSubtitle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(StatGap),
                modifier = Modifier.clearAndSetSemantics { contentDescription = statsDescription },
            ) {
                Icon(painterResource(AppIcons.Views), contentDescription = null, tint = color, modifier = Modifier.size(StatIconSize))
                Text(views, style = style, color = color, maxLines = 1)
                MetaSeparator(color)
                Icon(painterResource(AppIcons.Likes), contentDescription = null, tint = color, modifier = Modifier.size(StatIconSize))
                Text(likes, style = style, color = color, maxLines = 1)
            }
        }
    }
}

private val PillInset = 9.dp
private val StatGap = 4.dp
private val StatIconSize = 16.dp

/** `.pdur`: "▶ 2m 28s" pill on promo artwork. */
@Composable
private fun DurationPill(text: String, modifier: Modifier = Modifier) {
    val onArtwork = AppDesignSystem.extendedColors.onImageScrim
    Row(
        modifier = modifier
            .background(AppDesignSystem.extendedColors.artworkScrim.copy(alpha = PILL_ALPHA), CircleShape)
            .padding(horizontal = PillPaddingH, vertical = PillPaddingV),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(StatGap),
    ) {
        Icon(painterResource(AppIcons.Play), contentDescription = null, tint = onArtwork, modifier = Modifier.size(StatIconSize))
        Text(text, style = AppDesignSystem.textStyles.badge.copy(letterSpacing = AppDesignSystem.textStyles.cardSubtitle.letterSpacing), color = onArtwork)
    }
}

private val PillPaddingH = 8.dp
private val PillPaddingV = 3.dp

/**
 * Top-10 card (prototype `.t10`): an outlined Inter ExtraBold numeral with the 3:4 poster
 * overlapping it, bottoms aligned, and the bold title below the poster. The title extends
 * [TopTenTitleOverhang] below the card, into the rail's bottom padding.
 */
@Composable
internal fun TopTenCard(item: ContentItem, rank: Int, modifier: Modifier = Modifier) {
    val dimens = AppDesignSystem.dimens
    val description = stringResource(R.string.home_rank_title, rank, item.title)
    val twoDigits = rank >= 10
    val numeralHeight = dimens.topTenNumeralHeight
    val numeralWidth = numeralHeight * (if (twoDigits) RANK_DOUBLE_VIEWBOX_WIDTH else RANK_SINGLE_VIEWBOX_WIDTH) / RANK_VIEWBOX_HEIGHT
    val posterStart = numeralWidth - if (twoDigits) TopTenDoubleOverlap else dimens.topTenOverlap
    val posterHeight = dimens.topTenPosterWidth / TOP_TEN_POSTER_ASPECT_RATIO
    val cardHeight = maxOf(numeralHeight + TopTenNumeralBottom, posterHeight)
    Box(
        modifier
            .size(posterStart + dimens.topTenPosterWidth, cardHeight + TopTenTitleOverhang)
            .clearAndSetSemantics { contentDescription = description },
    ) {
        RankNumeral(
            rank = rank,
            modifier = Modifier
                .offset(y = cardHeight - TopTenNumeralBottom - numeralHeight)
                .size(numeralWidth, numeralHeight),
        )
        ContentImage(
            url = item.imageUrl,
            shape = AppDesignSystem.shapes.card,
            modifier = Modifier
                .offset(x = posterStart, y = cardHeight - posterHeight)
                .size(dimens.topTenPosterWidth, posterHeight),
        )
        Text(
            text = item.title,
            style = AppDesignSystem.textStyles.cardTitle.copy(fontWeight = FontWeight.Bold),
            color = AppDesignSystem.colors.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .offset(x = TopTenTitleStart, y = cardHeight + TopTenTitleTop)
                .width(dimens.portraitCardWidth),
        )
    }
}

/** `.t10num { margin-bottom: 6px }`. */
private val TopTenNumeralBottom = 6.dp

/**
 * The outlined rank numeral, drawn like the prototype SVG: `<text x="50%" y="204"
 * text-anchor="middle">` in a 210-unit-high viewBox scaled to the box, stroke only.
 */
@Composable
private fun RankNumeral(rank: Int, modifier: Modifier = Modifier) {
    val isDark = AppDesignSystem.isDark
    val ink = AppDesignSystem.colors.onSurface
    val color = if (isDark) ink.copy(alpha = RANK_OUTLINE_ALPHA_DARK) else ink.copy(alpha = RANK_OUTLINE_ALPHA_LIGHT)
    val style = AppDesignSystem.textStyles.rank
    val typeface = LocalFontFamilyResolver.current
        .resolve(style.fontFamily, style.fontWeight ?: FontWeight.ExtraBold)
        .value as? Typeface
    val text = rank.toString()
    val paint = remember(typeface, color, text) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            this.color = color.toArgb()
            this.style = Paint.Style.STROKE
            textAlign = Paint.Align.CENTER
            letterSpacing = if (text.length > 1) RANK_TRACKING_DOUBLE else RANK_TRACKING_SINGLE
        }
    }
    Canvas(modifier) {
        // One viewBox unit in px; font size, baseline and stroke all scale with it (sizes in dp, not sp).
        val unit = size.height / RANK_VIEWBOX_HEIGHT
        paint.textSize = RANK_FONT_SIZE * unit
        paint.strokeWidth = (if (isDark) RANK_STROKE_DARK else RANK_STROKE_LIGHT) * unit
        drawIntoCanvas { it.nativeCanvas.drawText(text, size.width / 2f, RANK_BASELINE * unit, paint) }
    }
}

/** Compact counts as in the prototype ("4.2M", "312K"), localized. */
internal fun formatCompact(value: Long): String =
    CompactDecimalFormat.getInstance(Locale.getDefault(), CompactDecimalFormat.CompactStyle.SHORT).format(value)
