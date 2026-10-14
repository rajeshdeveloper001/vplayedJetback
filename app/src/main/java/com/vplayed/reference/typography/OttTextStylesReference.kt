package com.vplayed.reference.typography

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.content.res.Configuration.UI_MODE_TYPE_NORMAL
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.vplayed.core.designsystem.component.AppSurface
import com.vplayed.core.designsystem.dimension.WindowWidthClass
import com.vplayed.core.designsystem.theme.AppDesignSystem
import com.vplayed.core.designsystem.theme.AppTheme
import com.vplayed.core.designsystem.typography.AppTextStyles

/**
 * Reference sheet for the OTT text styles (`AppDesignSystem.textStyles`): every role rendered with
 * a realistic sample and its resolved size/weight. Not product UI — use it to pick the right role
 * (header vs sub header, card title vs metadata, …) and to check sizes on phone vs tablet in the
 * previews below. Token values live in `core/designsystem/typography/AppTypography.kt`.
 */
@Composable
fun OttTextStylesReference(
    modifier: Modifier = Modifier,
    showSpecs: Boolean = true,
) {
    val styles = AppDesignSystem.textStyles
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(AppDesignSystem.dimens.spacing.xs)) {
        samples(styles).forEach { sample ->
            Column {
                when (sample.role) {
                    "header" -> HeaderWithLink(sample)
                    "badge" -> Badge(sample)
                    else -> Text(sample.text, style = sample.style, color = sample.color ?: Color.Unspecified)
                }
                if (showSpecs) SpecLabel(sample.role, sample.style)
            }
        }
    }
}

private data class Sample(val role: String, val text: String, val style: TextStyle, val color: Color? = null)

@Composable
private fun samples(styles: AppTextStyles): List<Sample> {
    val muted = AppDesignSystem.colors.onSurfaceVariant
    return listOf(
        Sample("heroTitle", "The Last Kingdom", styles.heroTitle),
        Sample("heroSubtitle", "Drama · Action · 2026", styles.heroSubtitle),
        Sample("screenHeader", "Movies", styles.screenHeader),
        Sample("header", "Trending Now", styles.header),
        Sample("subHeader", "Episodes", styles.subHeader),
        Sample("cardTitle", "Midnight Express", styles.cardTitle),
        Sample("cardSubtitle", "Thriller · S1 E4", styles.cardSubtitle, muted),
        Sample("metadata", "2026 · U/A 13+ · 2h 14m · Tamil", styles.metadata),
        Sample("synopsis", "A former detective returns to the city that forgot him to solve one last case.", styles.synopsis),
        Sample("badge", "LIVE", styles.badge),
        Sample("action", "Watch now", styles.action),
        Sample("playerTitle", "Midnight Express · S1 E4", styles.playerTitle),
        Sample("playerTimestamp", "01:02:45 / 02:14:00", styles.playerTimestamp),
        Sample("caption", "Subtitles available in 6 languages", styles.caption, muted),
    )
}

@Composable
private fun HeaderWithLink(sample: Sample) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(sample.text, style = sample.style, modifier = Modifier.weight(1f))
        Text("See all", style = AppDesignSystem.textStyles.sectionLink, color = AppDesignSystem.colors.primary)
    }
}

@Composable
private fun Badge(sample: Sample) {
    val spacing = AppDesignSystem.dimens.spacing
    Text(
        sample.text,
        style = sample.style,
        color = AppDesignSystem.colors.onError,
        modifier = Modifier
            .background(AppDesignSystem.colors.error, AppDesignSystem.shapes.extraSmall)
            .padding(horizontal = spacing.xs, vertical = spacing.xxs),
    )
}

@Composable
private fun SpecLabel(role: String, style: TextStyle) {
    Text(
        text = "$role · ${style.fontSize.value.toInt()}sp · ${style.fontWeight.label()}",
        style = AppDesignSystem.typography.labelSmall,
        color = AppDesignSystem.colors.outline,
    )
}

private fun FontWeight?.label(): String = when (this?.weight) {
    300 -> "Light"
    400 -> "Regular"
    500 -> "Medium"
    600 -> "SemiBold"
    700 -> "Bold"
    800 -> "ExtraBold"
    900 -> "Black"
    else -> this?.weight?.toString() ?: "Regular"
}

private const val DARK = UI_MODE_NIGHT_YES or UI_MODE_TYPE_NORMAL

@Composable
private fun ReferencePreview(widthClass: WindowWidthClass) {
    AppTheme(windowWidthClass = widthClass) {
        AppSurface {
            OttTextStylesReference(modifier = Modifier.padding(AppDesignSystem.dimens.spacing.md))
        }
    }
}

@Preview(name = "Phone (compact) · Light", group = "OTT text styles", widthDp = 400, heightDp = 1100)
@Preview(name = "Phone (compact) · Dark", group = "OTT text styles", widthDp = 400, heightDp = 1100, uiMode = DARK)
@Composable
private fun OttTextStylesPhonePreview() = ReferencePreview(WindowWidthClass.COMPACT)

@Preview(name = "Tablet (expanded) · Light", group = "OTT text styles", widthDp = 900, heightDp = 1200)
@Composable
private fun OttTextStylesTabletPreview() = ReferencePreview(WindowWidthClass.EXPANDED)
