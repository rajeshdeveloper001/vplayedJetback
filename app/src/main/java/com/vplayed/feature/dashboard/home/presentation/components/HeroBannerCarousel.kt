package com.vplayed.feature.dashboard.home.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.size.Size as CoilSize
import com.vplayed.R
import com.vplayed.core.designsystem.component.cssLinearGradient
import com.vplayed.core.designsystem.component.skeleton
import com.vplayed.core.designsystem.icon.AppIcons
import com.vplayed.core.designsystem.theme.AppDesignSystem
import com.vplayed.core.ui.rememberReducedMotion
import com.vplayed.feature.dashboard.home.domain.model.HomeBanner
import com.vplayed.feature.dashboard.home.domain.model.ShowType
import kotlin.math.max
import kotlin.math.min

private const val AUTO_ADVANCE_MILLIS = 6_000

/** `#hero { height: clamp(460px, 72vh, 600px) }`; in landscape it never exceeds 72% of the window. */
private const val HERO_WINDOW_FRACTION = 0.72f

/** Artwork focus: centre horizontally, 18% from the top (prototype `object-position: center 18%`). */
private val ArtworkAlignment = BiasAlignment(horizontalBias = 0f, verticalBias = -0.64f)

// `.hero-inner` padding and the vertical rhythm of `.hero-c`.
private val InnerPaddingH = 16.dp
private val InnerPaddingBottom = 36.dp
private val ChipBottom = 12.dp
private val TitleBottom = 14.dp
private val SynopsisBottom = 26.dp
private val ActionGap = 10.dp

/** `.hero h1`: clamp(30px, 9.5vw, 40px), line-height 0.98, -0.035em. */
private const val TITLE_VW = 0.095f
private const val TITLE_MIN = 30f
private const val TITLE_MAX = 40f
private const val TITLE_LINE_HEIGHT = 0.98f
private const val TITLE_TRACKING = -0.035f

/** `.hero-title-img`: max-width min(560px, 68vw); max-height clamp(96px, 12vw, 150px). */
private const val TITLE_IMAGE_MAX_WIDTH = 560f
private const val TITLE_IMAGE_VW = 0.68f
private const val TITLE_IMAGE_HEIGHT_VW = 0.12f
private const val TITLE_IMAGE_MIN_HEIGHT = 96f
private const val TITLE_IMAGE_MAX_HEIGHT = 150f

// `.badge-new`: neutral-950 55% pill, white 15% ring, 6×12 padding, 7dp yellow dot with an 8dp glow.
private const val CHIP_FILL_ALPHA = 0.55f
private const val CHIP_RING_ALPHA = 0.15f
private val ChipPaddingH = 12.dp
private val ChipPaddingV = 6.dp
private val ChipGap = 8.dp
private val ChipDot = 7.dp
private val ChipGlow = 8.dp

// `.hero-actions .btn-primary`: 24dp padding, 9dp icon gap, red glow 0 8 20 at 26%; the glass button is 48dp narrower.
private val PrimaryPaddingH = 24.dp
private val PrimaryIconGap = 9.dp
private val ButtonIconSize = 20.dp
private val PrimaryGlowBlur = 20.dp
private val PrimaryGlowY = 8.dp
private const val PRIMARY_GLOW_ALPHA = 0.26f
private val PrimaryExtraWidth = 48.dp

// `.hero-ctl .dash`: 7dp dots 5dp apart, active 20dp with a 6 s yellow fill; a 120dp window, 18dp from the bottom.
private val DashGap = 5.dp
private val DashWindow = 120.dp
private val DashBottom = 18.dp
private const val DASH_TRACK_ALPHA = 0.28f
private const val DASH_WINDOW_COUNT = 9
private const val DASH_LEAD = 4

// `.hero-scrim` (mobile): bottom-up page fade and a left-edge shade.
private const val SCRIM_82 = 0.82f
private const val SCRIM_32 = 0.32f
private const val SCRIM_45 = 0.45f
// Light theme (`body.light .hero-scrim`): the hero stays dark.
private const val LIGHT_SCRIM_72 = 0.72f
private const val LIGHT_SIDE_92 = 0.92f
private const val LIGHT_SIDE_74 = 0.74f
private const val LIGHT_SIDE_18 = 0.18f

/**
 * Home hero (prototype mobile `#hero`): swipeable full-bleed artwork under the prototype scrim,
 * a content-type chip (Video / Series / Micro Series) with the yellow dot, the title artwork (or
 * the title when there is none or it fails to load), a two-line synopsis, "Watch now" + glass "+"
 * and page dashes whose active dash fills over the 6 s auto-advance. Auto-advance stops while
 * dragging and when the user has animations turned off.
 */
@Composable
internal fun HeroBannerCarousel(
    banners: List<HomeBanner>,
    onWatchNow: (HomeBanner) -> Unit,
    onAddToList: (HomeBanner) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = AppDesignSystem.dimens
    val pagerState = rememberPagerState { banners.size }
    val isDragged by pagerState.interactionSource.collectIsDraggedAsState()
    val reducedMotion = rememberReducedMotion()
    val autoAdvance = !reducedMotion && banners.size > 1
    val progress = remember { Animatable(if (autoAdvance) 0f else 1f) }
    if (autoAdvance) {
        LaunchedEffect(pagerState.settledPage, isDragged) {
            progress.snapTo(0f)
            if (isDragged) return@LaunchedEffect
            progress.animateTo(1f, tween(AUTO_ADVANCE_MILLIS, easing = LinearEasing))
            pagerState.animateScrollToPage((pagerState.settledPage + 1) % banners.size)
        }
    }
    val position = stringResource(R.string.home_banner_position, pagerState.currentPage + 1, banners.size)
    val window = LocalWindowInfo.current.containerSize
    val windowHeight = with(LocalDensity.current) { window.height.toDp() }
    val landscape = window.width > window.height
    val heroHeight = (windowHeight * HERO_WINDOW_FRACTION)
        .coerceIn(dimens.heroMinHeight, dimens.heroMaxHeight)
        .let { if (landscape) it.coerceAtMost(windowHeight * HERO_WINDOW_FRACTION) else it }
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(heroHeight)
            .testTag(HomeTestTags.HERO)
            .semantics { stateDescription = position },
    ) {
        val width = maxWidth
        HorizontalPager(
            state = pagerState,
            key = { banners[it].id },
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            BannerPage(banners[page], width = width, onWatchNow = onWatchNow, onAddToList = onAddToList)
        }
        if (banners.size > 1) {
            PagerDashes(
                count = banners.size,
                selected = pagerState.currentPage,
                fill = { progress.value },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = DashBottom),
            )
        }
    }
}

@Composable
private fun BannerPage(banner: HomeBanner, width: Dp, onWatchNow: (HomeBanner) -> Unit, onAddToList: (HomeBanner) -> Unit) {
    Box(Modifier.fillMaxSize().skeleton(shape = AppDesignSystem.shapes.extraSmall)) {
        if (banner.imageUrl != null) {
            AsyncImage(
                model = banner.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = ArtworkAlignment,
                modifier = Modifier.fillMaxSize(),
            )
        }
        HeroScrim()
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = InnerPaddingH, end = InnerPaddingH, bottom = InnerPaddingBottom),
        ) {
            banner.showType?.let {
                HeroChip(stringResource(it.labelRes()))
                Spacer(Modifier.height(ChipBottom))
            }
            HeroTitle(banner, width)
            Spacer(Modifier.height(TitleBottom))
            banner.description?.let {
                Text(
                    text = it,
                    style = AppDesignSystem.textStyles.heroSubtitle,
                    color = AppDesignSystem.extendedColors.onImageScrimSoft,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(SynopsisBottom))
            }
            HeroActions(
                width = width - InnerPaddingH * 2,
                onWatchNow = { onWatchNow(banner) },
                onAddToList = { onAddToList(banner) },
            )
        }
    }
}

/** Text on the hero is light in both themes: the hero stays dark (prototype light theme too). */
@Composable
private fun heroTextColor(): Color =
    if (AppDesignSystem.isDark) AppDesignSystem.colors.onBackground else AppDesignSystem.extendedColors.onImageScrim

/** `.hero-scrim`: page color rising from the bottom plus a soft shade from the left edge. */
@Composable
private fun HeroScrim() {
    val ink = AppDesignSystem.extendedColors.artworkScrim
    val modifier = if (AppDesignSystem.isDark) {
        Modifier
            .background(cssLinearGradient(90f, 0f to ink.copy(alpha = SCRIM_45), 0.6f to Color.Transparent))
            .background(
                cssLinearGradient(
                    0f,
                    0.02f to AppDesignSystem.colors.background,
                    0.20f to ink.copy(alpha = SCRIM_82),
                    0.46f to ink.copy(alpha = SCRIM_32),
                    0.68f to Color.Transparent,
                ),
            )
    } else {
        Modifier
            .background(
                cssLinearGradient(
                    90f,
                    0f to ink.copy(alpha = LIGHT_SIDE_92),
                    0.28f to ink.copy(alpha = LIGHT_SIDE_74),
                    0.6f to ink.copy(alpha = LIGHT_SIDE_18),
                    0.82f to Color.Transparent,
                ),
            )
            .background(cssLinearGradient(0f, 0f to ink, 0.18f to ink.copy(alpha = LIGHT_SCRIM_72), 0.52f to Color.Transparent))
    }
    Box(Modifier.fillMaxSize().then(modifier))
}

/** Title artwork when it loads; the title text when there is no artwork or it fails (prototype `titleHTML`). */
@Composable
private fun HeroTitle(banner: HomeBanner, width: Dp) {
    val url = banner.titleImageUrl
    var failed by remember(url) { mutableStateOf(false) }
    // Natural artwork size, captured once: sizing the image from it must not restart the request.
    var natural by remember(url) { mutableStateOf<Size?>(null) }
    if (url == null || failed) {
        val size = (width.value * TITLE_VW).coerceIn(TITLE_MIN, TITLE_MAX)
        val style = AppDesignSystem.textStyles.heroTitle
        Text(
            text = banner.title,
            style = style.copy(
                fontSize = style.fontSize * (size / style.fontSize.value),
                lineHeight = TITLE_LINE_HEIGHT.em,
                letterSpacing = TITLE_TRACKING.em,
            ),
            color = heroTextColor(),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    } else {
        val context = LocalContext.current
        // Decode at the original size so the layout size below never changes the request.
        val request = remember(url) { ImageRequest.Builder(context).data(url).size(CoilSize.ORIGINAL).build() }
        val maxWidth = min(TITLE_IMAGE_MAX_WIDTH, width.value * TITLE_IMAGE_VW)
        val maxHeight = (width.value * TITLE_IMAGE_HEIGHT_VW).coerceIn(TITLE_IMAGE_MIN_HEIGHT, TITLE_IMAGE_MAX_HEIGHT)
        // CSS sizes the artwork at its natural pixel size (1px = 1dp) within the max box, keeping its ratio.
        val box = natural?.let { minOf(1f, maxWidth / it.width, maxHeight / it.height).let { scale -> (it.width * scale) to (it.height * scale) } }
        AsyncImage(
            model = request,
            contentDescription = banner.title,
            contentScale = ContentScale.Fit,
            alignment = Alignment.BottomStart,
            onSuccess = { success ->
                val size = success.painter.intrinsicSize
                if (natural == null && size.width > 0f && size.height > 0f) natural = size
            },
            onError = { failed = true },
            // Reserve the artwork's max height while it loads so the hero content does not jump.
            modifier = if (box == null) {
                Modifier.width(maxWidth.dp).height(maxHeight.dp)
            } else {
                Modifier.width(box.first.dp).height(box.second.dp)
            },
        )
    }
}

/** `.badge-new`: content type in an uppercase pill with the glowing yellow dot. */
@Composable
private fun HeroChip(text: String) {
    val extended = AppDesignSystem.extendedColors
    val yellow = AppDesignSystem.colors.tertiary
    Row(
        modifier = Modifier
            .background(extended.artworkScrim.copy(alpha = CHIP_FILL_ALPHA), CircleShape)
            .border(AppDesignSystem.dimens.borderThin, extended.onImageScrim.copy(alpha = CHIP_RING_ALPHA), CircleShape)
            .padding(horizontal = ChipPaddingH, vertical = ChipPaddingV),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ChipGap),
    ) {
        Box(
            Modifier
                .size(ChipDot)
                .dropShadow(CircleShape, Shadow(radius = ChipGlow, color = yellow))
                .background(yellow, CircleShape),
        )
        Text(text.uppercase(), style = AppDesignSystem.textStyles.badge, color = heroTextColor())
    }
}

/** `.hero-actions`: red "Watch now" and a glass "+" sharing the row, the primary 48dp wider. */
@Composable
private fun HeroActions(width: Dp, onWatchNow: () -> Unit, onAddToList: () -> Unit) {
    val dimens = AppDesignSystem.dimens
    val colors = AppDesignSystem.colors
    val extended = AppDesignSystem.extendedColors
    val glassWidth = max(0f, ((width - ActionGap - PrimaryExtraWidth) / 2).value).dp
    val shape = AppDesignSystem.shapes.button
    Row(horizontalArrangement = Arrangement.spacedBy(ActionGap), verticalAlignment = Alignment.CenterVertically) {
        Button(
            onClick = onWatchNow,
            shape = shape,
            colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary),
            contentPadding = PaddingValues(horizontal = PrimaryPaddingH),
            modifier = Modifier
                .width(glassWidth + PrimaryExtraWidth)
                .height(dimens.heroButtonHeight)
                .dropShadow(shape, Shadow(radius = PrimaryGlowBlur, color = colors.primary.copy(alpha = PRIMARY_GLOW_ALPHA), offset = DpOffset(0.dp, PrimaryGlowY))),
        ) {
            Icon(painterResource(AppIcons.Play), contentDescription = null, modifier = Modifier.size(ButtonIconSize))
            Text(
                text = stringResource(R.string.home_watch_now),
                style = AppDesignSystem.textStyles.action,
                maxLines = 1,
                modifier = Modifier.padding(start = PrimaryIconGap),
            )
        }
        Button(
            onClick = onAddToList,
            shape = shape,
            colors = ButtonDefaults.buttonColors(containerColor = extended.glassFill, contentColor = extended.onImageScrim),
            border = BorderStroke(dimens.borderThin, extended.glassRing),
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.width(glassWidth).height(dimens.heroButtonHeight),
        ) {
            Icon(painterResource(AppIcons.Add), contentDescription = stringResource(R.string.home_add_to_list))
        }
    }
}

/** `.hero-ctl .dashes`: dots with the active one a 20dp track filling yellow; scrolls when there are many. */
@Composable
private fun PagerDashes(count: Int, selected: Int, fill: () -> Float, modifier: Modifier = Modifier) {
    val dimens = AppDesignSystem.dimens
    val track = AppDesignSystem.extendedColors.onImageScrim.copy(alpha = DASH_TRACK_ALPHA)
    val yellow = AppDesignSystem.colors.tertiary
    val step = dimens.pagerDotSize + DashGap
    val first = if (count > DASH_WINDOW_COUNT) (selected - DASH_LEAD).coerceIn(0, count - DASH_WINDOW_COUNT) else 0
    val total = step * (count - 1) + dimens.pagerDotActiveWidth
    Box(modifier.width(minOf(total, DashWindow)).clip(RoundedCornerShape(0.dp))) {
        Row(
            modifier = Modifier.offset(x = -step * first).width(total),
            horizontalArrangement = Arrangement.spacedBy(DashGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(count) { index ->
                if (index == selected) {
                    val shape = RoundedCornerShape(dimens.pagerDotSize / 2)
                    Box(
                        Modifier
                            .width(dimens.pagerDotActiveWidth)
                            .height(dimens.pagerDotSize)
                            // Own layer: the 6 s fill redraws only this dash, not the hero artwork and shadows.
                            .graphicsLayer()
                            .clip(shape)
                            .background(track)
                            // Read in the draw phase only: the fill animates every frame without recomposition.
                            .drawBehind { drawRect(yellow, size = size.copy(width = size.width * fill())) },
                    )
                } else {
                    Box(Modifier.size(dimens.pagerDotSize).background(track, CircleShape))
                }
            }
        }
    }
}

private fun ShowType.labelRes(): Int = when (this) {
    ShowType.VIDEO -> R.string.home_show_type_video
    ShowType.SERIES -> R.string.home_show_type_series
    ShowType.MICRO_SERIES -> R.string.home_show_type_micro_series
}
