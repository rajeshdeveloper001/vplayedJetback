package com.vplayed.core.designsystem.color

import androidx.annotation.StringRes
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.vplayed.core.designsystem.component.mixWith

/**
 * A complete brand color definition: the Material 3 [ColorScheme] for light and dark mode plus
 * the status colors Material does not define. Add a new brand by creating another instance in
 * [BrandPalettes] — no screen or component code changes.
 */
@Immutable
data class BrandPalette(
    val id: String,
    @param:StringRes val nameRes: Int,
    val lightScheme: ColorScheme,
    val darkScheme: ColorScheme,
    val lightStatus: StatusColors = DefaultStatusColors.light,
    val darkStatus: StatusColors = DefaultStatusColors.dark,
)

/** Feedback colors (success / warning / info) that complement Material's error colors. */
@Immutable
data class StatusColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val info: Color,
    val onInfo: Color,
    val infoContainer: Color,
    val onInfoContainer: Color,
)

/**
 * Application semantic colors beyond Material's [ColorScheme]. Material roles (primary, surface,
 * outline, error, …) are read from `AppDesignSystem.colors`; these are read from
 * `AppDesignSystem.extendedColors`.
 */
@Immutable
data class AppExtendedColors(
    val status: StatusColors,
    /** Content color for disabled elements. */
    val disabled: Color,
    /** Container color for disabled elements. */
    val disabledContainer: Color,
    val divider: Color,
    /** Translucent layer drawn over content, e.g. behind a loading state or on video thumbnails. */
    val overlay: Color,
    /** Shimmer placeholder fill and its moving highlight. */
    val skeletonBase: Color,
    val skeletonHighlight: Color,
    /** Strong scrim behind text drawn on images (offer cards, hero), and the content on it. */
    val imageScrim: Color,
    val onImageScrim: Color,
    /** Secondary text on dark artwork (hero synopsis), in both themes. */
    val onImageScrimSoft: Color,
    /** Prototype secondary text (neutral-300 on dark) and faint text (neutral-500). */
    val textSoft: Color,
    val textFaint: Color,
    /** Prototype 1dp card/poster outline (--outline-default). */
    val hairline: Color,
    /** Translucent fill and 1dp ring of glass buttons (--glass-fill / 40% ring). */
    val glassFill: Color,
    val glassRing: Color,
    /** Dark translucent base for chips and badges drawn on artwork (both themes). */
    val artworkScrim: Color,
    /** Brand yellow accent (prototype --secondary) and the ink drawn on it. */
    val accentYellow: Color,
    val onAccentYellow: Color,
    /** Fixed dark tones under brand-tinted tiles (channel tiles): neutral-850 and neutral-950. */
    val darkTileHigh: Color,
    val darkTileLow: Color,
    /** Vinyl disc of music cards: groove and ridge rings (`--vinyl-a` / `--vinyl-b`). */
    val vinylGroove: Color,
    val vinylRidge: Color,
    /** Sign-in screen surfaces, fields and text ladder (prototype "VPlayed Sign Up"). */
    val auth: AuthColors,
    /**
     * Lighter brand accent for small emphasized text on dark (prototype `--brand-2`: step label,
     * field error); the primary color itself on light, where the lighter tint lacks contrast.
     */
    val formAccent: Color,
) {
    val success: Color get() = status.success
    val onSuccess: Color get() = status.onSuccess
    val warning: Color get() = status.warning
    val onWarning: Color get() = status.onWarning
    val info: Color get() = status.info
    val onInfo: Color get() = status.onInfo
}

private const val DISABLED_CONTENT_ALPHA = 0.38f
private const val DISABLED_CONTAINER_ALPHA = 0.12f
private const val OVERLAY_ALPHA = 0.32f
private const val IMAGE_SCRIM_ALPHA = 0.64f

/** Text and icons drawn on darkened images are white in every theme. */
private val OnImageScrim = Color(0xFFFFFFFF)

private const val GLASS_FILL_ALPHA = 0.12f
private const val GLASS_RING_ALPHA = 0.4f

/** Derives the extended colors from whichever scheme is active (brand or dynamic). */
fun extendedColorsFor(scheme: ColorScheme, status: StatusColors, isDark: Boolean = true): AppExtendedColors = AppExtendedColors(
    status = status,
    disabled = scheme.onSurface.copy(alpha = DISABLED_CONTENT_ALPHA),
    disabledContainer = scheme.onSurface.copy(alpha = DISABLED_CONTAINER_ALPHA),
    divider = scheme.outlineVariant,
    overlay = scheme.scrim.copy(alpha = OVERLAY_ALPHA),
    skeletonBase = scheme.surfaceContainerHigh,
    skeletonHighlight = scheme.surfaceContainerHighest,
    imageScrim = scheme.scrim.copy(alpha = IMAGE_SCRIM_ALPHA),
    onImageScrim = OnImageScrim,
    onImageScrimSoft = PrototypeNeutrals.TextSoftOnDark,
    textSoft = if (isDark) PrototypeNeutrals.TextSoftOnDark else PrototypeNeutrals.TextSoftOnLight,
    textFaint = PrototypeNeutrals.TextFaint,
    hairline = if (isDark) PrototypeNeutrals.HairlineOnDark else PrototypeNeutrals.HairlineOnLight,
    glassFill = PrototypeNeutrals.White.copy(alpha = GLASS_FILL_ALPHA),
    glassRing = PrototypeNeutrals.White.copy(alpha = GLASS_RING_ALPHA),
    artworkScrim = PrototypeNeutrals.ArtworkScrimBase,
    accentYellow = PrototypeNeutrals.Yellow,
    onAccentYellow = PrototypeNeutrals.YellowInk,
    darkTileHigh = PrototypeNeutrals.OfferEnd,
    darkTileLow = PrototypeNeutrals.ArtworkScrimBase,
    vinylGroove = if (isDark) scheme.surfaceContainerHigh else scheme.surfaceContainerHighest,
    vinylRidge = if (isDark) PrototypeNeutrals.Neutral700 else scheme.surfaceContainerHigh,
    auth = if (isDark) AuthSurfaces.dark else AuthSurfaces.light,
    formAccent = if (isDark) scheme.primary.mixWith(PrototypeNeutrals.White, FORM_ACCENT_FRACTION) else scheme.primary,
)

/** `--brand-2` (#FF2E4E) is the brand red mixed with 15% white. */
private const val FORM_ACCENT_FRACTION = 0.85f

/**
 * Sign-in screen colors (prototype "VPlayed Sign Up", dark default and `body.light`): backdrop,
 * glass modal and card, fields, social buttons, icon tiles and the four-step text ladder
 * (`--text-0` … `--text-3`). Brand glows and the primary button use the color scheme.
 */
@Immutable
data class AuthColors(
    val canvas: Color,
    val modalFillStart: Color,
    val modalFillEnd: Color,
    val modalBorder: Color,
    val cardFillStart: Color,
    val cardFillEnd: Color,
    val cardBorder: Color,
    /** 1dp highlight along the card's top edge. */
    val cardHighlight: Color,
    val cardShadow: Color,
    val fieldFill: Color,
    val fieldBorder: Color,
    val fieldBorderFocused: Color,
    /** 4dp halo around a focused field (neutral: fields never get a red focus). */
    val focusRing: Color,
    val fieldError: Color,
    val socialFill: Color,
    val socialBorder: Color,
    /** Close button and feature icon tiles. */
    val iconTileFill: Color,
    val iconTileBorder: Color,
    val textStrong: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
)

/**
 * One color variant of the prototype bulk-purchase card (`.premium-in.vpb-*`): background
 * gradient start, its end stop and glow, border, icon tile gradient and ink, overline, and button
 * colors (dark theme values).
 */
@Immutable
data class PromoAccentColors(
    /** The variant's base hue (light-theme tints are mixed from it). */
    val accent: Color,
    val backgroundStart: Color,
    val backgroundEnd: Color,
    /** Where the background reaches [backgroundEnd] (0–1). */
    val backgroundEndStop: Float,
    /** Radial glow from the left edge; transparent when the variant has none. */
    val glow: Color,
    val border: Color,
    val iconStart: Color,
    val iconEnd: Color,
    val iconInk: Color,
    val iconShadow: Color,
    val overline: Color,
    val buttonFill: Color,
    val buttonInk: Color,
)
