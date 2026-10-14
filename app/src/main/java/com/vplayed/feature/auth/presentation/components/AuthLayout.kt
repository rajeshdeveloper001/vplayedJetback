package com.vplayed.feature.auth.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.vplayed.core.designsystem.component.cssLinearGradient
import com.vplayed.core.designsystem.theme.AppDesignSystem

/** Which prototype layout fits the window. */
internal enum class AuthLayoutMode {
    /** Prototype ≤ 860px / Mobile page: one card, no marketing column. */
    SingleCard,

    /** Prototype > 860px: marketing column beside the card. */
    TwoColumn,
}

// Prototype body padding (40/24), modal minimum height, card paddings per breakpoint.
private val PageVerticalPadding = 40.dp
private val PageHorizontalPadding = 24.dp
private val CompactHeightPagePadding = 16.dp
private val ModalMinHeight = 680.dp
// 20dp in the prototype, minus the 5dp margin of the 48dp touch target around the 38dp button.
private val CloseButtonInset = 15.dp
private val NarrowWindowMaxWidth = 400.dp
private val WideModalBreakpoint = 1100.dp
// Card padding (horizontal × vertical): narrow phones 20 × 30, single card 32 × 40,
// two-column 28 × 40, two-column above 1100dp 64 × 56.
private val NarrowCardVerticalPadding = 30.dp
private val TwoColumnCardHorizontalPadding = 28.dp
private val WideCardHorizontalPadding = 64.dp
private val WideCardVerticalPadding = 56.dp
private val CardShadowElevation = 24.dp
private val ModalShadowElevation = 40.dp
private val InnerCardCorner = 18.dp
private const val AMBIENT_GLOW_ALPHA = 0.26f
private const val AMBIENT_GLOW_MID_ALPHA = 0.08f
private val AmbientGlowRadius = 280.dp
private val CompactHeight = 480.dp

internal fun authLayoutModeFor(width: Dp, height: Dp, twoColumnMinWidth: Dp): AuthLayoutMode =
    if (width >= twoColumnMinWidth && height >= CompactHeight) AuthLayoutMode.TwoColumn else AuthLayoutMode.SingleCard

/**
 * Shell shared by the sign-in screens (prototype "VPlayed Sign Up"): the glow backdrop and either a
 * centered glass card (phones, foldables, landscape phones) or a two-column modal with the marketing
 * column (wide windows). The page scrolls when the window, or the space left by the keyboard, is
 * shorter than the content. [content] is the card's form; it is vertically centered.
 */
@Composable
internal fun AuthLayout(
    onClose: () -> Unit,
    screenTag: String,
    cardTag: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.(AuthLayoutMode) -> Unit,
) {
    val dimens = AppDesignSystem.dimens
    Box(
        modifier = modifier
            .fillMaxSize()
            .loginBackdrop()
            .testTag(screenTag),
    ) {
        // Measured inside the system bars, cutouts and keyboard, so the card re-centers and the
        // page scrolls when the keyboard opens.
        BoxWithConstraints(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            val mode = authLayoutModeFor(maxWidth, maxHeight, dimens.authTwoColumnMinWidth)
            val verticalPadding = if (maxHeight < CompactHeight) CompactHeightPagePadding else PageVerticalPadding
            val contentHeight = maxHeight - verticalPadding * 2
            val windowWidth = maxWidth
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight)
                    .padding(horizontal = PageHorizontalPadding, vertical = verticalPadding),
                contentAlignment = Alignment.Center,
            ) {
                when (mode) {
                    AuthLayoutMode.SingleCard -> SingleCard(onClose, cardTag, windowWidth, content)
                    AuthLayoutMode.TwoColumn -> TwoColumnModal(onClose, cardTag, windowWidth, contentHeight, content)
                }
            }
        }
    }
}

@Composable
private fun SingleCard(
    onClose: () -> Unit,
    cardTag: String,
    windowWidth: Dp,
    content: @Composable ColumnScope.(AuthLayoutMode) -> Unit,
) {
    val dimens = AppDesignSystem.dimens
    val shape = RoundedCornerShape(dimens.authCornerRadius)
    val narrow = windowWidth <= NarrowWindowMaxWidth
    Box(
        modifier = Modifier
            .widthIn(max = dimens.authCardMaxWidth)
            .fillMaxWidth()
            .modalSurface(shape)
            .cardSurface(shape),
    ) {
        Column(
            modifier = Modifier
                .padding(
                    horizontal = if (narrow) dimens.spacing.lg else dimens.spacing.xxl,
                    vertical = if (narrow) NarrowCardVerticalPadding else dimens.spacing.xxxl,
                )
                .testTag(cardTag),
            verticalArrangement = Arrangement.Center,
        ) { content(AuthLayoutMode.SingleCard) }
        LoginCloseButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = CloseButtonInset, end = CloseButtonInset),
        )
    }
}

@Composable
private fun TwoColumnModal(
    onClose: () -> Unit,
    cardTag: String,
    windowWidth: Dp,
    availableHeight: Dp,
    content: @Composable ColumnScope.(AuthLayoutMode) -> Unit,
) {
    val dimens = AppDesignSystem.dimens
    val spacing = dimens.spacing
    val shape = RoundedCornerShape(dimens.authCornerRadius)
    val wide = windowWidth > WideModalBreakpoint
    Box(
        modifier = Modifier
            .widthIn(max = dimens.authModalMaxWidth)
            .fillMaxWidth()
            .modalSurface(shape),
    ) {
        // Intrinsic height lets the card column fill the modal, which is at least 680dp tall
        // (or the available height on shorter windows) and grows with its content.
        Row(
            modifier = Modifier
                .heightIn(min = min(ModalMinHeight, availableHeight))
                .height(IntrinsicSize.Min),
        ) {
            LoginShowcasePanel(
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically)
                    .padding(start = spacing.huge, end = spacing.huge, top = spacing.huge, bottom = spacing.xxxl),
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(if (wide) spacing.xxl else spacing.lg),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .cardSurface(RoundedCornerShape(InnerCardCorner))
                        .padding(
                            horizontal = if (wide) WideCardHorizontalPadding else TwoColumnCardHorizontalPadding,
                            vertical = if (wide) WideCardVerticalPadding else spacing.xxxl,
                        )
                        .testTag(cardTag),
                    verticalArrangement = Arrangement.Center,
                ) { content(AuthLayoutMode.TwoColumn) }
            }
        }
        LoginCloseButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = CloseButtonInset, end = CloseButtonInset),
        )
    }
}

/** Prototype `.modal`: diagonal glass gradient, 1dp border and a deep shadow. */
@Composable
private fun Modifier.modalSurface(shape: Shape): Modifier {
    val auth = AppDesignSystem.extendedColors.auth
    return this
        .shadow(ModalShadowElevation, shape, ambientColor = auth.cardShadow, spotColor = auth.cardShadow)
        .clip(shape)
        .background(cssLinearGradient(135f, 0f to auth.modalFillStart, 1f to auth.modalFillEnd))
        .border(AppDesignSystem.dimens.borderThin, auth.modalBorder, shape)
}

/** Prototype `.right-card`: glass fill, 1dp border, top-edge highlight and the red ambient glow. */
@Composable
private fun Modifier.cardSurface(shape: Shape): Modifier {
    val auth = AppDesignSystem.extendedColors.auth
    val brand = AppDesignSystem.colors.primary
    val hairline = AppDesignSystem.dimens.borderThin
    return this
        .shadow(CardShadowElevation, shape, ambientColor = auth.cardShadow, spotColor = auth.cardShadow)
        .clip(shape)
        .background(cssLinearGradient(135f, 0f to auth.cardFillStart, 1f to auth.cardFillEnd))
        // Drawn over the fill, under the content (prototype `.ambient-red` z-index 0, content 2).
        .drawBehind {
            val radius = AmbientGlowRadius.toPx()
            drawCircle(
                brush = Brush.radialGradient(
                    0f to brand.copy(alpha = AMBIENT_GLOW_ALPHA),
                    0.42f to brand.copy(alpha = AMBIENT_GLOW_MID_ALPHA),
                    0.7f to Color.Transparent,
                    center = center,
                    radius = radius,
                ),
                radius = radius,
            )
            // 1dp highlight on the top edge, from 20% to 80% of the width.
            drawLine(
                brush = Brush.horizontalGradient(
                    0f to Color.Transparent,
                    0.5f to auth.cardHighlight,
                    1f to Color.Transparent,
                    startX = size.width * 0.2f,
                    endX = size.width * 0.8f,
                ),
                start = Offset(size.width * 0.2f, 0f),
                end = Offset(size.width * 0.8f, 0f),
                strokeWidth = hairline.toPx(),
            )
        }
        .border(hairline, auth.cardBorder, shape)
}
