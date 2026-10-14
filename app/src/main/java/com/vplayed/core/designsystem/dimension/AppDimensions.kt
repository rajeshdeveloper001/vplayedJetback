package com.vplayed.core.designsystem.dimension

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Spacing scale. Use these instead of literal dp values for padding and gaps. */
@Immutable
data class Spacing(
    val none: Dp = 0.dp,
    val xxs: Dp = 4.dp,
    val xs: Dp = 8.dp,
    val sm: Dp = 12.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 20.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    val xxxl: Dp = 40.dp,
    val huge: Dp = 48.dp,
)

@Immutable
data class IconSizes(
    val small: Dp = 16.dp,
    val medium: Dp = 24.dp,
    val large: Dp = 32.dp,
    val illustration: Dp = 64.dp,
)

/** Component density. Compact reduces component heights; touch targets never drop below 48dp. */
enum class UiDensity { COMFORTABLE, COMPACT }

/**
 * Layout and component sizing tokens. Screen padding and max widths that depend on window size
 * live in `core.ui.AppLayoutInfo`, which scales these base values per window class.
 */
@Immutable
data class AppDimensions(
    val spacing: Spacing = Spacing(),
    val icon: IconSizes = IconSizes(),
    val screenHorizontalPadding: Dp = 16.dp,
    val screenVerticalPadding: Dp = 16.dp,
    val cardCornerRadius: Dp = 12.dp,
    val cardContentPadding: Dp = 16.dp,
    val cardSpacing: Dp = 12.dp,
    val buttonHeight: Dp = 48.dp,
    val inputFieldHeight: Dp = 56.dp,
    val toolbarHeight: Dp = 64.dp,
    val bottomNavigationHeight: Dp = 80.dp,
    val navigationRailWidth: Dp = 80.dp,
    val dialogPadding: Dp = 24.dp,
    val bottomSheetPadding: Dp = 24.dp,
    val listItemHeight: Dp = 56.dp,
    val minTouchTarget: Dp = 48.dp,
    val dividerThickness: Dp = 1.dp,
    val borderThin: Dp = 1.dp,
    val borderThick: Dp = 2.dp,
    /** Maximum width of reading content (forms, text) on wide windows. */
    val readableContentMaxWidth: Dp = 640.dp,
    /** Maximum width of grid/list content on very wide windows. */
    val contentMaxWidth: Dp = 1200.dp,
    /** Minimum card width in adaptive grids: 1 column on phones, 2–3 on tablets and in landscape. */
    val adaptiveGridMinCellWidth: Dp = 320.dp,
    /**
     * Home (prototype mobile Home at 427dp). Card widths grow on medium/expanded windows
     * (`...Large`); everything else is the prototype's computed size.
     */
    val landscapeCardWidth: Dp = 158.dp,
    val landscapeCardWidthLarge: Dp = 232.dp,
    val portraitCardWidth: Dp = 112.dp,
    val portraitCardWidthLarge: Dp = 152.dp,
    /** Top 10 (`.t10`): 3:4 poster, the outlined numeral box behind it and their overlap. */
    val topTenPosterWidth: Dp = 128.dp,
    val topTenNumeralWidth: Dp = 120.dp,
    val topTenNumeralHeight: Dp = 168.dp,
    val topTenOverlap: Dp = 34.dp,
    /** Live card (`.lcard`) and its 32dp channel logo well. */
    val liveCardWidth: Dp = 300.dp,
    val liveLogoSize: Dp = 32.dp,
    /** Channel tile (`.chtile`) and its 60dp logo well. */
    val channelTileWidth: Dp = 300.dp,
    val channelTileMinHeight: Dp = 256.dp,
    val channelLogoSize: Dp = 60.dp,
    /** Music card (`.mcard`): square cover holding the vinyl disc. */
    val musicCardWidth: Dp = 140.dp,
    /** Bulk-purchase promo (`.premium-in`): window width minus this inset, capped; 60dp icon tile. */
    val offerCardInset: Dp = 72.dp,
    val offerCardMaxWidth: Dp = 420.dp,
    val offerIconSize: Dp = 60.dp,
    /** Hero: tall full-bleed artwork on phones, clamped between these heights. */
    val heroMinHeight: Dp = 460.dp,
    val heroMaxHeight: Dp = 600.dp,
    val heroButtonHeight: Dp = 50.dp,
    /** Row header (`.row-head`) height; chevron touch target uses the same size. */
    val rowHeaderHeight: Dp = 44.dp,
    /** Compact buttons on cards (`.lbtn`, Follow) and the promo CTA. */
    val cardButtonHeight: Dp = 44.dp,
    val promoButtonHeight: Dp = 46.dp,
    /** Hero page dashes: 7dp dots, active 20dp. */
    val pagerDotSize: Dp = 7.dp,
    val pagerDotActiveWidth: Dp = 20.dp,
    val topBarLogoHeight: Dp = 24.dp,
    /** Brand logo width on the splash screen (compact windows / medium and wider). */
    val splashLogoWidth: Dp = 180.dp,
    val splashLogoWidthLarge: Dp = 260.dp,
    /**
     * Sign-in (prototype "VPlayed Sign Up"): single card up to [authCardMaxWidth]; two-column modal
     * up to [authModalMaxWidth] from [authTwoColumnMinWidth] (prototype breakpoint 861px).
     */
    val authCardMaxWidth: Dp = 460.dp,
    val authModalMaxWidth: Dp = 1280.dp,
    val authTwoColumnMinWidth: Dp = 861.dp,
    val authCornerRadius: Dp = 24.dp,
    /** Field, primary button and social button minimum heights (52 / 54 / 50 in the prototype). */
    val formFieldMinHeight: Dp = 52.dp,
    val formActionMinHeight: Dp = 54.dp,
    val socialButtonMinHeight: Dp = 50.dp,
    /** Round close button and square feature icon tiles (38dp). */
    val authIconButtonSize: Dp = 38.dp,
    val authLogoHeight: Dp = 46.dp,
    /** Round back button of the verification screens (prototype ≤ 900px `.astep-back` disc). */
    val authBackButtonSize: Dp = 40.dp,
    /** Verification-code boxes (prototype `.otp-row`): square, up to 56dp (46dp on narrow cards), 10dp apart (7dp). */
    val otpBoxMaxSize: Dp = 56.dp,
    val otpBoxMaxSizeNarrow: Dp = 46.dp,
    val otpBoxGap: Dp = 10.dp,
    val otpBoxGapNarrow: Dp = 7.dp,
    /** Success step: check circle and the "Taking you in…" progress bar (prototype `.success-check`, `.vpa-bar`). */
    val authSuccessIconSize: Dp = 84.dp,
    val authSuccessBarWidth: Dp = 160.dp,
    val authSuccessBarHeight: Dp = 3.dp,
    /** Visible height of the "Sign out" button on a device row (the touch target stays 48dp). */
    val deviceSignOutHeight: Dp = 36.dp,
) {
    companion object {
        val Comfortable = AppDimensions()
        val Compact = AppDimensions(
            cardContentPadding = 12.dp,
            cardSpacing = 8.dp,
            buttonHeight = 40.dp,
            inputFieldHeight = 48.dp,
            toolbarHeight = 56.dp,
            listItemHeight = 48.dp,
        )

        fun forDensity(density: UiDensity): AppDimensions = when (density) {
            UiDensity.COMFORTABLE -> Comfortable
            UiDensity.COMPACT -> Compact
        }
    }
}
