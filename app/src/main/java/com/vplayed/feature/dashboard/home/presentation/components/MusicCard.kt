package com.vplayed.feature.dashboard.home.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.vplayed.core.designsystem.icon.AppIcons
import com.vplayed.core.designsystem.theme.AppDesignSystem
import com.vplayed.feature.dashboard.home.domain.model.ContentItem
import com.vplayed.feature.dashboard.home.domain.model.ItemDetails

// Prototype `.mcard` (disc 94% of the cover, label 46% of the disc, 7dp spindle hole).
private const val DISC_FRACTION = 0.94f
private const val LABEL_FRACTION = 0.46f
private val GrooveWidth = 2.dp
private val GroovePitch = 3.4.dp
private val LabelRing = 4.dp
private val HoleSize = 7.dp
private val HoleRing = 2.dp
private const val HOLE_RING_ALPHA = 0.55f
private const val RIM_ALPHA = 0.16f
private const val SHADOW_ALPHA = 0.45f
private val DiscShadowBlur = 26.dp
private val DiscShadowY = 10.dp
private val LabelShadowBlur = 10.dp
private val LabelShadowY = 2.dp

/** `.mc-disc::after`: conic glare from 210° — light 16% fading by 55°, a soft shade around 300°. */
private const val GLARE_START_DEGREES = 210f
private const val GLARE_LIGHT_ALPHA = 0.16f
private const val GLARE_SHADE_ALPHA = 0.12f

private val MetaTop = 12.dp
private val MetaInset = 4.dp
private val ArtistTop = 3.dp
private val ArtistGap = 5.dp
private val NoteSize = 20.dp

/** Audio track (prototype `.mcard`): the cover is the label of a grooved vinyl disc; centered title and artist. */
@Composable
internal fun MusicCard(item: ContentItem, modifier: Modifier = Modifier) {
    val artist = (item.details as? ItemDetails.Audio)?.artist
    Column(
        modifier = modifier
            .width(AppDesignSystem.dimens.musicCardWidth)
            .semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
            VinylDisc(item.imageUrl, Modifier.fillMaxSize(DISC_FRACTION))
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = MetaTop, start = MetaInset, end = MetaInset),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = item.title,
                style = AppDesignSystem.textStyles.cardTitle.copy(letterSpacing = (-0.01).em),
                color = AppDesignSystem.colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            if (artist != null) {
                Row(
                    modifier = Modifier.padding(top = ArtistTop),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(ArtistGap),
                ) {
                    Icon(painterResource(AppIcons.MusicNote), contentDescription = null, tint = AppDesignSystem.colors.tertiary, modifier = Modifier.size(NoteSize))
                    Text(
                        text = artist,
                        style = AppDesignSystem.textStyles.cardSubtitle,
                        color = AppDesignSystem.extendedColors.textFaint,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun VinylDisc(imageUrl: String?, modifier: Modifier = Modifier) {
    val extended = AppDesignSystem.extendedColors
    val groove = extended.vinylGroove
    val ridge = extended.vinylRidge
    val rim = extended.onImageScrim.copy(alpha = RIM_ALPHA)
    val shadowColor = AppDesignSystem.colors.scrim.copy(alpha = SHADOW_ALPHA)
    val glare = Brush.sweepGradient(
        0f to extended.onImageScrim.copy(alpha = GLARE_LIGHT_ALPHA),
        55f / 360f to Color.Transparent,
        210f / 360f to Color.Transparent,
        300f / 360f to AppDesignSystem.colors.scrim.copy(alpha = GLARE_SHADE_ALPHA),
        1f to Color.Transparent,
    )
    Box(
        modifier.dropShadow(CircleShape, Shadow(radius = DiscShadowBlur, color = shadowColor, offset = DpOffset(0.dp, DiscShadowY))),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f
            drawCircle(groove, radius)
            // repeating-radial-gradient: 2dp groove, then ridge up to 3.4dp, repeated outwards.
            val pitch = GroovePitch.toPx()
            val ridgeWidth = pitch - GrooveWidth.toPx()
            var r = GrooveWidth.toPx() + ridgeWidth / 2f
            while (r < radius) {
                drawCircle(ridge, r, style = Stroke(ridgeWidth))
                r += pitch
            }
            drawCircle(rim, radius - 0.5.dp.toPx(), style = Stroke(1.dp.toPx()))
        }
        ContentImage(
            url = imageUrl,
            shape = CircleShape,
            bordered = false,
            modifier = Modifier
                .fillMaxSize(LABEL_FRACTION)
                // `box-shadow: 0 0 0 4px` ring outside the label, then its soft shadow.
                .drawBehind { drawCircle(groove, radius = size.minDimension / 2f + LabelRing.toPx()) }
                .dropShadow(CircleShape, Shadow(radius = LabelShadowBlur, color = shadowColor, offset = DpOffset(0.dp, LabelShadowY))),
        )
        // CSS conic angles start at 12 o'clock; Compose sweeps start at 3 o'clock.
        Canvas(Modifier.fillMaxSize().rotate(GLARE_START_DEGREES - QUARTER_TURN)) {
            drawCircle(glare)
        }
        Box(
            Modifier
                .size(HoleSize + HoleRing * 2)
                .border(HoleRing, AppDesignSystem.colors.tertiary.copy(alpha = HOLE_RING_ALPHA), CircleShape)
                .padding(HoleRing)
                .background(groove, CircleShape),
        )
    }
}

private const val QUARTER_TURN = 90f
