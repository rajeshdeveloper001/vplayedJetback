package com.vplayed.feature.dashboard.home.presentation.components

import android.content.Context
import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.vplayed.R
import com.vplayed.core.designsystem.icon.AppIcons
import com.vplayed.core.designsystem.theme.AppDesignSystem
import com.vplayed.shared.home.ContentItem
import com.vplayed.shared.home.ItemDetails
import com.vplayed.shared.home.LiveStatus
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Prototype `.lcard` measurements (427dp phone).
private val BadgeInset = 14.dp
private val BadgePaddingH = 11.dp
private val BadgePaddingV = 6.dp
private val BadgeGap = 7.dp
private val BadgeDot = 7.dp
private val OverlayPaddingH = 16.dp
private val OverlayPaddingV = 12.dp
private val OverlayBottom = 14.dp
private val SubTop = 7.dp
private val SubGap = 6.dp
private val ClockSize = 20.dp
private val FooterPaddingH = 12.dp
private val FooterPaddingV = 11.dp
private val FooterGap = 12.dp
private val LogoGap = 10.dp
private val ButtonPaddingH = 16.dp
private val ButtonGap = 8.dp
private val ButtonIconSize = 20.dp

/** `.lfade`: neutral-950 94% at 3% → 25% at 40% → transparent at 64% (bottom to top). */
private const val FADE_STRONG = 0.94f
private const val FADE_SOFT = 0.25f
private const val UPCOMING_FILL_ALPHA = 0.72f
private const val UPCOMING_BORDER_ALPHA = 0.15f
private const val LOGO_RING_ALPHA = 0.06f
private const val MAX_INITIALS = 2

/**
 * Live stream card (prototype `.lcard`): 16:9 artwork with a LIVE / UPCOMING badge, title and
 * start time over a dark fade, then a footer with the channel logo well and name and the action
 * ("Watch Live" in brand red, or a white "Set reminder" for scheduled streams).
 */
@Composable
internal fun LiveCard(item: ContentItem, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    val live = item.details as? ItemDetails.Live ?: ItemDetails.Live(LiveStatus.ENDED, null, null)
    val shape = AppDesignSystem.shapes.tile
    val extended = AppDesignSystem.extendedColors
    Column(
        modifier = modifier
            .width(AppDesignSystem.dimens.liveCardWidth)
            .clip(shape)
            .background(AppDesignSystem.colors.surfaceContainerLow)
            .border(AppDesignSystem.dimens.borderThin, extended.hairline, shape),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(CARD_ASPECT_RATIO)
                .semantics(mergeDescendants = true) {},
        ) {
            ContentImage(url = item.imageUrl, shape = RectangleShape, bordered = false, modifier = Modifier.fillMaxSize())
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.36f to Color.Transparent,
                            0.60f to extended.artworkScrim.copy(alpha = FADE_SOFT),
                            0.97f to extended.artworkScrim.copy(alpha = FADE_STRONG),
                        ),
                    ),
            )
            when (live.status) {
                LiveStatus.LIVE -> LiveBadge(Modifier.padding(BadgeInset))
                LiveStatus.UPCOMING -> UpcomingBadge(Modifier.padding(BadgeInset))
                LiveStatus.ENDED -> Unit
            }
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = OverlayBottom)
                    .padding(horizontal = OverlayPaddingH, vertical = OverlayPaddingV),
            ) {
                Text(
                    text = item.title,
                    style = AppDesignSystem.textStyles.cardTitle.copy(letterSpacing = TitleTracking),
                    color = extended.onImageScrim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                scheduleText(live)?.let { schedule ->
                    Row(
                        modifier = Modifier.padding(top = SubTop),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(SubGap),
                    ) {
                        Icon(painterResource(AppIcons.Schedule), contentDescription = null, tint = extended.onImageScrim, modifier = Modifier.size(ClockSize))
                        Text(schedule, style = AppDesignSystem.textStyles.cardSubtitle, color = extended.onImageScrim, maxLines = 1)
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FooterPaddingH, vertical = FooterPaddingV),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(FooterGap),
        ) {
            Row(
                modifier = Modifier.weight(1f).semantics(mergeDescendants = true) {},
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LogoGap),
            ) {
                live.channelName?.let { name ->
                    LogoWell(initials(name))
                    Text(
                        text = name,
                        style = AppDesignSystem.textStyles.cardTitle.copy(lineHeight = AppDesignSystem.textStyles.cardTitle.fontSize),
                        color = AppDesignSystem.colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            LiveAction(live.status, onClick)
        }
    }
}

/** `.ltitle` letter-spacing -0.01em. */
private val TitleTracking = (-0.01).em

/** `.lfch-logo`: 32dp white well with the channel initials (logos are not in the API yet). */
@Composable
private fun LogoWell(initials: String) {
    val shape = AppDesignSystem.shapes.card
    val ink = AppDesignSystem.extendedColors.darkTileHigh
    Box(
        modifier = Modifier
            .size(AppDesignSystem.dimens.liveLogoSize)
            .background(AppDesignSystem.extendedColors.onImageScrim, shape)
            .border(AppDesignSystem.dimens.borderThin, ink.copy(alpha = LOGO_RING_ALPHA), shape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            style = AppDesignSystem.textStyles.badge.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = LogoTracking, lineHeight = AppDesignSystem.textStyles.badge.fontSize),
            color = ink,
            maxLines = 1,
        )
    }
}

/** `.lfch-logo` letter-spacing 0.02em. */
private val LogoTracking = 0.02.em

/** `.lbtn`: 44dp button — red "Watch Live" (play), white "Set reminder" (bell). */
@Composable
private fun LiveAction(status: LiveStatus, onClick: () -> Unit) {
    val colors = AppDesignSystem.colors
    val extended = AppDesignSystem.extendedColors
    val upcoming = status == LiveStatus.UPCOMING
    val container = if (upcoming) extended.onImageScrim else colors.primary
    val content = if (upcoming) extended.artworkScrim else colors.onPrimary
    val label = when (status) {
        LiveStatus.LIVE -> R.string.home_watch_live
        LiveStatus.UPCOMING -> R.string.home_set_reminder
        LiveStatus.ENDED -> R.string.home_watch_now
    }
    Button(
        onClick = onClick,
        shape = AppDesignSystem.shapes.button,
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
        contentPadding = PaddingValues(horizontal = ButtonPaddingH),
        modifier = Modifier.height(AppDesignSystem.dimens.cardButtonHeight),
    ) {
        Icon(
            painterResource(if (upcoming) AppIcons.Notifications else AppIcons.Play),
            contentDescription = null,
            modifier = Modifier.size(ButtonIconSize),
        )
        Text(
            text = stringResource(label),
            style = AppDesignSystem.textStyles.actionSmall,
            modifier = Modifier.padding(start = ButtonGap),
            maxLines = 1,
        )
    }
}

/** "● LIVE" tag on live artwork (prototype `.lbadge.event`). */
@Composable
internal fun LiveBadge(modifier: Modifier = Modifier) {
    val onPrimary = AppDesignSystem.colors.onPrimary
    Row(
        modifier = modifier
            .background(AppDesignSystem.colors.primary, AppDesignSystem.shapes.card)
            .padding(horizontal = BadgePaddingH, vertical = BadgePaddingV),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BadgeGap),
    ) {
        Box(Modifier.size(BadgeDot).background(onPrimary, CircleShape))
        Text(text = stringResource(R.string.home_badge_live), style = AppDesignSystem.textStyles.tag, color = onPrimary)
    }
}

/** "UPCOMING" tag (prototype `.lbadge.chan`): dark glass with a thin light ring. */
@Composable
private fun UpcomingBadge(modifier: Modifier = Modifier) {
    val extended = AppDesignSystem.extendedColors
    val shape = AppDesignSystem.shapes.card
    Text(
        text = stringResource(R.string.home_badge_upcoming),
        style = AppDesignSystem.textStyles.tag,
        color = extended.onImageScrim,
        modifier = modifier
            .background(extended.artworkScrim.copy(alpha = UPCOMING_FILL_ALPHA), shape)
            .border(AppDesignSystem.dimens.borderThin, extended.onImageScrim.copy(alpha = UPCOMING_BORDER_ALPHA), shape)
            .padding(horizontal = BadgePaddingH, vertical = BadgePaddingV),
    )
}

/** "Started 8:10 PM", "Starts 9:00 PM", "Tomorrow · 9:15 PM" (prototype `.lsub`), in the device time zone. */
@Composable
private fun scheduleText(live: ItemDetails.Live): String? {
    val millis = live.startsAtMillis ?: return null
    val context = LocalContext.current
    val time = DateFormat.getTimeFormat(context).format(Date(millis))
    val dayOffset = daysFromToday(millis)
    val dateAndTime = if (dayOffset == 0) time else "${shortDate(context, millis)} · $time"
    return when (live.status) {
        LiveStatus.LIVE -> stringResource(R.string.home_live_started, dateAndTime)
        LiveStatus.UPCOMING -> if (dayOffset == 1) {
            stringResource(R.string.home_live_tomorrow, time)
        } else {
            stringResource(R.string.home_live_starts, dateAndTime)
        }
        LiveStatus.ENDED -> stringResource(R.string.home_live_streamed, shortDate(context, millis))
    }
}

private fun shortDate(context: Context, millis: Long): String = DateUtils.formatDateTime(
    context,
    millis,
    DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH or DateUtils.FORMAT_NO_YEAR,
)

private fun daysFromToday(millis: Long): Int {
    fun Calendar.startOfDay() = apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val today = Calendar.getInstance().startOfDay()
    val day = Calendar.getInstance().apply { timeInMillis = millis }.startOfDay()
    return ((day - today) / DateUtils.DAY_IN_MILLIS).toInt()
}

/** "VplayedDev" → "VP", "VPlayed Originals" → "VO" (prototype logo wells show initials). */
internal fun initials(name: String): String {
    val words = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    val letters = if (words.size > 1) words.take(MAX_INITIALS).map { it.first() } else words.firstOrNull()?.take(MAX_INITIALS)?.toList().orEmpty()
    return letters.joinToString("").uppercase(Locale.getDefault())
}
