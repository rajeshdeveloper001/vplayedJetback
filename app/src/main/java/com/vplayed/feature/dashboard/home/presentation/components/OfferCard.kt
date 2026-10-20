package com.vplayed.feature.dashboard.home.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.vplayed.core.designsystem.color.PromoAccentColors
import com.vplayed.core.designsystem.color.PromoAccents
import com.vplayed.core.designsystem.component.cssLinearGradient
import com.vplayed.core.designsystem.component.cssRadialGradient
import com.vplayed.core.designsystem.component.mixWith
import com.vplayed.core.designsystem.icon.AppIcons
import com.vplayed.core.designsystem.theme.AppDesignSystem
import com.vplayed.core.ui.localizedNumber
import com.vplayed.shared.home.BundleAccent
import com.vplayed.shared.home.ContentItem
import com.vplayed.shared.home.ItemDetails
import com.vplayed.shared.home.Price
import com.vplayed.shared.l10n.Res
import com.vplayed.shared.l10n.home_bundle_days
import com.vplayed.shared.l10n.home_explore_bundles
import com.vplayed.shared.l10n.home_save_up_to
import com.vplayed.shared.l10n.nav_bulk_purchase
import java.text.NumberFormat
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

// Prototype `.premium-in` (mobile): 24×26 padding, 16dp column gap, 60dp icon tile.
private val CardPaddingV = 24.dp
private val CardPaddingH = 26.dp
private val SectionGap = 16.dp
private val TitleGap = 8.dp
private val BodyBottom = 24.dp
private val PriceGap = 14.dp
private val ButtonPaddingH = 28.dp
private val IconTileShadowBlur = 22.dp
private val IconTileShadowY = 8.dp
private const val BACKGROUND_ANGLE = 100f
private const val ICON_ANGLE = 150f
private const val GLOW_RADIUS_X = 1.2f
private const val GLOW_RADIUS_Y = 1.6f
private const val GLOW_STOP = 0.58f

// Light theme (`body.light .premium-in`): a faint tint from the top and a softer glow.
private const val LIGHT_TINT = 0.06f
private const val LIGHT_GLOW = 0.14f
private const val LIGHT_GLOW_STOP = 0.6f
private const val LIGHT_BORDER = 0.3f

/**
 * Bulk-purchase bundle (prototype `.premium-in`): accent icon tile, overline (bundle label),
 * name, description, price with the struck original and "Save up to N%", and a full-width
 * "Explore bundles" button. The color variant comes from the bundle's banner artwork.
 */
@Composable
internal fun OfferCard(item: ContentItem, width: Dp, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    val bundle = item.details as? ItemDetails.Bundle
    val accent = accentColors(bundle?.accent ?: BundleAccent.VIOLET)
    val isDark = AppDesignSystem.isDark
    val shape = AppDesignSystem.shapes.promo
    val surface = AppDesignSystem.colors.surface
    val background = if (isDark) {
        listOf(
            cssRadialGradient(GLOW_RADIUS_X, GLOW_RADIUS_Y, 0f, 0.5f, 0f to accent.glow, GLOW_STOP to Color.Transparent),
            cssLinearGradient(BACKGROUND_ANGLE, 0f to accent.backgroundStart, accent.backgroundEndStop to accent.backgroundEnd, 1f to accent.backgroundEnd),
        )
    } else {
        listOf(
            cssRadialGradient(GLOW_RADIUS_X, GLOW_RADIUS_Y, 0f, 0.5f, 0f to accent.accent.copy(alpha = LIGHT_GLOW), LIGHT_GLOW_STOP to Color.Transparent),
            cssLinearGradient(180f, 0f to accent.accent.mixWith(surface, LIGHT_TINT), 1f to surface),
        )
    }
    val border = if (isDark) accent.border else accent.accent.copy(alpha = LIGHT_BORDER)
    Column(
        modifier = modifier
            .width(width)
            .clip(shape)
            .background(background[1])
            .background(background[0])
            .border(AppDesignSystem.dimens.borderThin, border, shape)
            .padding(horizontal = CardPaddingH, vertical = CardPaddingV),
    ) {
        Column(Modifier.semantics(mergeDescendants = true) {}) {
            IconTile(accent)
            Spacer(Modifier.height(SectionGap))
            Text(
                text = (bundle?.label ?: stringResource(Res.string.nav_bulk_purchase)).uppercase(),
                style = AppDesignSystem.textStyles.badge,
                color = if (isDark) accent.overline else accent.accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.title,
                style = AppDesignSystem.textStyles.cardHeadline,
                color = AppDesignSystem.colors.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(vertical = TitleGap),
            )
            bundle?.description?.let {
                Text(
                    text = it,
                    style = AppDesignSystem.textStyles.cardBody,
                    color = AppDesignSystem.extendedColors.textSoft,
                    // Two lines always, so cards in one row keep the same height (the prototype's flex stretch).
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(BodyBottom))
        }
        Spacer(Modifier.height(SectionGap))
        bundle?.let { PriceLine(it) }
        Spacer(Modifier.height(PriceGap))
        Button(
            onClick = onClick,
            shape = AppDesignSystem.shapes.button,
            colors = ButtonDefaults.buttonColors(containerColor = accent.buttonFill, contentColor = accent.buttonInk),
            contentPadding = PaddingValues(horizontal = ButtonPaddingH),
            modifier = Modifier.fillMaxWidth().height(AppDesignSystem.dimens.promoButtonHeight),
        ) {
            Text(stringResource(Res.string.home_explore_bundles), style = AppDesignSystem.textStyles.actionSmall, maxLines = 1)
        }
    }
}

/** `.pmed`: 60dp rounded tile with the accent gradient, a soft colored shadow and the bag icon. */
@Composable
private fun IconTile(accent: PromoAccentColors) {
    val shape = AppDesignSystem.shapes.panel
    Box(
        modifier = Modifier
            .size(AppDesignSystem.dimens.offerIconSize)
            .dropShadow(shape, Shadow(radius = IconTileShadowBlur, color = accent.iconShadow, offset = DpOffset(0.dp, IconTileShadowY)))
            .clip(shape)
            .background(cssLinearGradient(ICON_ANGLE, 0f to accent.iconStart, 1f to accent.iconEnd)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(AppIcons.ShoppingBag),
            contentDescription = null,
            tint = accent.iconInk,
            modifier = Modifier.size(AppDesignSystem.dimens.icon.large),
        )
    }
}

/** "₹1,872  ₹2,080  ·  Save up to 10%" — amount, struck original and the saving (or validity). */
@Composable
private fun PriceLine(bundle: ItemDetails.Bundle) {
    val styles = AppDesignSystem.textStyles
    val extended = AppDesignSystem.extendedColors
    val price = bundle.price ?: return
    val discountPercent = bundle.discountPercent
    val days = bundle.days
    val sub = when {
        discountPercent != null -> stringResource(Res.string.home_save_up_to, localizedNumber(discountPercent))
        days != null -> pluralStringResource(Res.plurals.home_bundle_days, days, localizedNumber(days))
        else -> null
    }
    val text = buildAnnotatedString {
        withStyle(styles.cardHeadline.toSpanStyle().copy(color = AppDesignSystem.colors.onSurface)) { append(formatPrice(price)) }
        bundle.originalPrice?.let {
            append("  ")
            withStyle(
                styles.cardSubtitle.toSpanStyle().copy(
                    fontWeight = FontWeight.Medium,
                    color = extended.textFaint,
                    textDecoration = TextDecoration.LineThrough,
                ),
            ) { append(formatPrice(it)) }
        }
        sub?.let {
            withStyle(styles.cardSubtitle.toSpanStyle().copy(color = extended.textSoft)) { append("   ·   $it") }
        }
    }
    Text(text = text, style = styles.cardHeadline)
}

private fun formatPrice(price: Price): String {
    val format = NumberFormat.getNumberInstance().apply { maximumFractionDigits = if (price.amount % 1.0 == 0.0) 0 else 2 }
    return price.currencySign + format.format(price.amount)
}

private fun accentColors(accent: BundleAccent): PromoAccentColors = when (accent) {
    BundleAccent.BLUE -> PromoAccents.Blue
    BundleAccent.GOLD -> PromoAccents.Gold
    BundleAccent.VIOLET -> PromoAccents.Violet
    BundleAccent.GREEN -> PromoAccents.Green
}
