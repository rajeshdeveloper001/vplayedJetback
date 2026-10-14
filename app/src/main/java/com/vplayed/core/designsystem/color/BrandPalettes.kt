package com.vplayed.core.designsystem.color

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.vplayed.R

/**
 * Color values for every bundled brand palette. This is the only file that contains raw color
 * literals. Values follow the VPlayed prototype design tokens: black surfaces in dark mode,
 * white surfaces in light mode, brand red (`#FF0935`, from the logo) as the action color.
 */
object BrandPalettes {

    /** Dark mode: near-black surface ladder (prototype neutral-950 … neutral-700), white content. */
    private val BlackSurfaces = darkColorScheme(
        background = Color(0xFF0A0A0B),
        onBackground = Color(0xFFF6F7F9),
        surface = Color(0xFF0A0A0B),
        onSurface = Color(0xFFF6F7F9),
        surfaceVariant = Color(0xFF26282E),
        onSurfaceVariant = Color(0xFF8B919C),
        surfaceDim = Color(0xFF0A0A0B),
        surfaceBright = Color(0xFF26282E),
        surfaceContainerLowest = Color(0xFF050506),
        surfaceContainerLow = Color(0xFF131316),
        surfaceContainer = Color(0xFF1B1C21),
        surfaceContainerHigh = Color(0xFF26282E),
        surfaceContainerHighest = Color(0xFF2E3037),
        inverseSurface = Color(0xFFECEEF1),
        inverseOnSurface = Color(0xFF131316),
        outline = Color(0xFF646A75),
        outlineVariant = Color(0xFF26282E),
        scrim = Color(0xFF000000),
        error = Color(0xFFFF5470),
        onError = Color(0xFF2A0008),
        errorContainer = Color(0xFF5C0411),
        onErrorContainer = Color(0xFFFFD9DF),
    )

    /** Light mode: white surface ladder (prototype neutral-0 … neutral-200), ink content. */
    private val WhiteSurfaces = lightColorScheme(
        background = Color(0xFFFFFFFF),
        onBackground = Color(0xFF111216),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF111216),
        surfaceVariant = Color(0xFFECEEF1),
        onSurfaceVariant = Color(0xFF646A75),
        surfaceDim = Color(0xFFECEEF1),
        surfaceBright = Color(0xFFFFFFFF),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF6F7F9),
        surfaceContainer = Color(0xFFF4F5F7),
        surfaceContainerHigh = Color(0xFFECEEF1),
        surfaceContainerHighest = Color(0xFFD9DCE1),
        inverseSurface = Color(0xFF1B1C21),
        inverseOnSurface = Color(0xFFF6F7F9),
        outline = Color(0xFF8B919C),
        outlineVariant = Color(0xFFD9DCE1),
        scrim = Color(0xFF000000),
        error = Color(0xFFD32F2F),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFE4E8),
        onErrorContainer = Color(0xFF7A0012),
    )

    /** Brand: red actions on black (dark) or white (light). The default palette. */
    val Vplayed = BrandPalette(
        id = "vplayed",
        nameRes = R.string.palette_vplayed,
        lightScheme = WhiteSurfaces.copy(
            // Red-600 instead of the logo red keeps red text ≥ 4.5:1 on white.
            primary = Color(0xFFE20831),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFFFD9DF),
            onPrimaryContainer = Color(0xFF5C0411),
            inversePrimary = Color(0xFFFF7D90),
            surfaceTint = Color(0xFFE20831),
            secondary = Color(0xFF26282E),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFECEEF1),
            onSecondaryContainer = Color(0xFF111216),
            tertiary = Color(0xFF876900),
            onTertiary = Color(0xFFFFFFFF),
            tertiaryContainer = Color(0xFFFFF3C4),
            onTertiaryContainer = Color(0xFF5C4700),
        ),
        darkScheme = BlackSurfaces.copy(
            primary = Color(0xFFFF0935),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFF5C0411),
            onPrimaryContainer = Color(0xFFFFD9DF),
            inversePrimary = Color(0xFFE20831),
            surfaceTint = Color(0xFFFF0935),
            secondary = Color(0xFFECEEF1),
            onSecondary = Color(0xFF131316),
            secondaryContainer = Color(0xFF26282E),
            onSecondaryContainer = Color(0xFFFFFFFF),
            tertiary = Color(0xFFFFD119),
            onTertiary = Color(0xFF16140A),
            tertiaryContainer = Color(0xFF5C4700),
            onTertiaryContainer = Color(0xFFFFF3C4),
        ),
    )

    /** Black and white only: white actions on black (dark) or black actions on white (light). */
    val Monochrome = BrandPalette(
        id = "monochrome",
        nameRes = R.string.palette_monochrome,
        lightScheme = WhiteSurfaces.copy(
            primary = Color(0xFF0A0A0B),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFECEEF1),
            onPrimaryContainer = Color(0xFF0A0A0B),
            inversePrimary = Color(0xFFFFFFFF),
            surfaceTint = Color(0xFF0A0A0B),
            secondary = Color(0xFF3F444D),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFECEEF1),
            onSecondaryContainer = Color(0xFF111216),
            tertiary = Color(0xFF646A75),
            onTertiary = Color(0xFFFFFFFF),
            tertiaryContainer = Color(0xFFD9DCE1),
            onTertiaryContainer = Color(0xFF111216),
        ),
        darkScheme = BlackSurfaces.copy(
            primary = Color(0xFFFFFFFF),
            onPrimary = Color(0xFF0A0A0B),
            primaryContainer = Color(0xFF26282E),
            onPrimaryContainer = Color(0xFFFFFFFF),
            inversePrimary = Color(0xFF0A0A0B),
            surfaceTint = Color(0xFFFFFFFF),
            secondary = Color(0xFFD9DCE1),
            onSecondary = Color(0xFF131316),
            secondaryContainer = Color(0xFF26282E),
            onSecondaryContainer = Color(0xFFFFFFFF),
            tertiary = Color(0xFFB7BCC4),
            onTertiary = Color(0xFF131316),
            tertiaryContainer = Color(0xFF3F444D),
            onTertiaryContainer = Color(0xFFFFFFFF),
        ),
    )
}

/**
 * Prototype neutrals that have no Material role: secondary text (neutral-300 / neutral-600),
 * faint text (neutral-500), the hairline outline, glass fills and the translucent black used
 * behind chips and badges on artwork. Read them through `AppDesignSystem.extendedColors`.
 */
object PrototypeNeutrals {
    val TextSoftOnDark = Color(0xFFB7BCC4)
    val TextSoftOnLight = Color(0xFF3F444D)
    val TextFaint = Color(0xFF646A75)
    val Neutral700 = Color(0xFF3F444D)
    val HairlineOnDark = Color(0x14FFFFFF)
    val HairlineOnLight = Color(0x1A111216)
    /** neutral-950: translucent dark behind chips/badges drawn on artwork (both themes). */
    val ArtworkScrimBase = Color(0xFF0A0A0B)
    val White = Color(0xFFFFFFFF)
    /** Brand yellow (prototype --secondary / yellow-500). */
    val Yellow = Color(0xFFFFD119)
    val YellowInk = Color(0xFF16140A)
    /** Bulk-purchase card accents (prototype vpb-blue / gold / violet / festive). */
    val OfferBlue = Color(0xFF3B82F6)
    val OfferBlueTint = Color(0xFF669EF8)
    val OfferBlueText = Color(0xFF76A7F9)
    val OfferBlueStart = Color(0xFF0F1623)
    val OfferGoldStart = Color(0xFF342D16)
    val OfferViolet = Color(0xFF6E56CF)
    val OfferVioletTint = Color(0xFFA596E1)
    val OfferVioletStart = Color(0xFF14111F)
    val OfferGreen = Color(0xFF22C55E)
    val OfferGreenTint = Color(0xFF1EAD53)
    val OfferGreenText = Color(0xFF57D385)
    val OfferGreenStart = Color(0xFF0C1D13)
    val OfferEnd = Color(0xFF131316)
}

/** Bulk-purchase card variants (prototype `.premium-in.vpb-blue/gold/violet/festive`). */
object PromoAccents {
    private const val GLOW_ALPHA = 0.2f
    private const val BORDER_ALPHA = 0.34f
    private const val SHADOW_ALPHA = 0.28f
    private val Ink = Color(0xFF0D1C36)
    private val ButtonInk = PrototypeNeutrals.OfferEnd

    val Blue = PromoAccentColors(
        accent = PrototypeNeutrals.OfferBlue,
        backgroundStart = PrototypeNeutrals.OfferBlueStart,
        backgroundEnd = PrototypeNeutrals.OfferEnd,
        backgroundEndStop = 0.46f,
        glow = PrototypeNeutrals.OfferBlue.copy(alpha = GLOW_ALPHA),
        border = PrototypeNeutrals.OfferBlue.copy(alpha = BORDER_ALPHA),
        iconStart = PrototypeNeutrals.OfferBlueTint,
        iconEnd = PrototypeNeutrals.OfferBlue,
        iconInk = Ink,
        iconShadow = PrototypeNeutrals.OfferBlue.copy(alpha = SHADOW_ALPHA),
        overline = PrototypeNeutrals.OfferBlueText,
        buttonFill = PrototypeNeutrals.White,
        buttonInk = ButtonInk,
    )
    val Gold = PromoAccentColors(
        accent = PrototypeNeutrals.Yellow,
        backgroundStart = PrototypeNeutrals.OfferGoldStart,
        backgroundEnd = PrototypeNeutrals.OfferEnd,
        backgroundEndStop = 0.6f,
        glow = Color.Transparent,
        border = PrototypeNeutrals.HairlineOnDark,
        iconStart = PrototypeNeutrals.Yellow,
        iconEnd = PrototypeNeutrals.Yellow,
        iconInk = PrototypeNeutrals.YellowInk,
        iconShadow = PrototypeNeutrals.Yellow.copy(alpha = SHADOW_ALPHA),
        overline = PrototypeNeutrals.Yellow,
        buttonFill = PrototypeNeutrals.Yellow,
        buttonInk = PrototypeNeutrals.YellowInk,
    )
    val Violet = PromoAccentColors(
        accent = PrototypeNeutrals.OfferViolet,
        backgroundStart = PrototypeNeutrals.OfferVioletStart,
        backgroundEnd = PrototypeNeutrals.OfferEnd,
        backgroundEndStop = 0.46f,
        glow = PrototypeNeutrals.OfferViolet.copy(alpha = 0.22f),
        border = PrototypeNeutrals.OfferViolet.copy(alpha = 0.36f),
        iconStart = PrototypeNeutrals.OfferVioletTint,
        iconEnd = PrototypeNeutrals.OfferViolet,
        iconInk = Color(0xFF1A1532),
        iconShadow = PrototypeNeutrals.OfferViolet.copy(alpha = SHADOW_ALPHA),
        overline = PrototypeNeutrals.OfferVioletTint,
        buttonFill = PrototypeNeutrals.White,
        buttonInk = ButtonInk,
    )
    val Green = PromoAccentColors(
        accent = PrototypeNeutrals.OfferGreen,
        backgroundStart = PrototypeNeutrals.OfferGreenStart,
        backgroundEnd = PrototypeNeutrals.OfferEnd,
        backgroundEndStop = 0.46f,
        glow = PrototypeNeutrals.OfferGreen.copy(alpha = 0.24f),
        border = PrototypeNeutrals.OfferGreen.copy(alpha = 0.38f),
        iconStart = PrototypeNeutrals.OfferGreenTint,
        iconEnd = PrototypeNeutrals.OfferGreen,
        iconInk = PrototypeNeutrals.White,
        iconShadow = PrototypeNeutrals.OfferGreen.copy(alpha = SHADOW_ALPHA),
        overline = PrototypeNeutrals.OfferGreenText,
        buttonFill = PrototypeNeutrals.OfferGreen,
        buttonInk = PrototypeNeutrals.White,
    )
}

/** Sign-in screen colors (prototype "VPlayed Sign Up"): dark default and `body.light`. */
object AuthSurfaces {
    val dark = AuthColors(
        canvas = Color(0xFF08080A),
        modalFillStart = Color(0xB81C1C20),
        modalFillEnd = Color(0xC7121216),
        modalBorder = Color(0x1FFFFFFF),
        cardFillStart = Color(0x0FFFFFFF),
        cardFillEnd = Color(0x0FFFFFFF),
        cardBorder = Color(0x1FFFFFFF),
        cardHighlight = Color(0x4DFFFFFF),
        cardShadow = Color(0x59000000),
        fieldFill = Color(0x0FFFFFFF),
        fieldBorder = Color(0x33FFFFFF),
        fieldBorderFocused = Color(0x99DCE0E8),
        focusRing = Color(0x29DCE0E8),
        fieldError = Color(0xFFEF4444),
        socialFill = Color(0x0FFFFFFF),
        socialBorder = Color(0x1FFFFFFF),
        iconTileFill = Color(0x0FFFFFFF),
        iconTileBorder = Color(0x1FFFFFFF),
        textStrong = Color(0xFFFFFFFF),
        textPrimary = Color(0xFFD9DCE1),
        textSecondary = Color(0xFFB7BCC4),
        textMuted = Color(0xFF8B919C),
    )
    val light = AuthColors(
        canvas = Color(0xFFECEEF1),
        modalFillStart = Color(0xDBFFFFFF),
        modalFillEnd = Color(0xDBFFFFFF),
        modalBorder = Color(0x0F111216),
        cardFillStart = Color(0xE6FFFFFF),
        cardFillEnd = Color(0xB3FFFFFF),
        cardBorder = Color(0x1A111216),
        cardHighlight = Color(0xFFFFFFFF),
        cardShadow = Color(0x1A111216),
        fieldFill = Color(0xFFFFFFFF),
        fieldBorder = Color(0x2E0A0A0B),
        fieldBorderFocused = Color(0x610A0A0B),
        // The prototype's light ring (#DCE0E8 at 16%) is invisible on white; neutral-950 at 8% instead.
        focusRing = Color(0x140A0A0B),
        fieldError = Color(0xFFEF4444),
        socialFill = Color(0xFFFFFFFF),
        socialBorder = Color(0x240A0A0B),
        iconTileFill = Color(0x0F111216),
        iconTileBorder = Color(0x240A0A0B),
        textStrong = Color(0xFF141518),
        textPrimary = Color(0xFF3F444D),
        textSecondary = Color(0xFF646A75),
        textMuted = Color(0xFF8B919C),
    )
}

/** Status colors shared by all bundled palettes unless a palette overrides them. */
object DefaultStatusColors {
    val light = StatusColors(
        success = Color(0xFF15803D),
        onSuccess = Color(0xFFFFFFFF),
        successContainer = Color(0xFFDCFCE7),
        onSuccessContainer = Color(0xFF0B3D1E),
        warning = Color(0xFF876900),
        onWarning = Color(0xFFFFFFFF),
        warningContainer = Color(0xFFFFF3C4),
        onWarningContainer = Color(0xFF2C2000),
        info = Color(0xFF1D5FD1),
        onInfo = Color(0xFFFFFFFF),
        infoContainer = Color(0xFFDBE8FE),
        onInfoContainer = Color(0xFF0A2A66),
    )
    val dark = StatusColors(
        success = Color(0xFF22C55E),
        onSuccess = Color(0xFF052E14),
        successContainer = Color(0xFF14532D),
        onSuccessContainer = Color(0xFFDCFCE7),
        warning = Color(0xFFFFD119),
        onWarning = Color(0xFF16140A),
        warningContainer = Color(0xFF5C4700),
        onWarningContainer = Color(0xFFFFF3C4),
        info = Color(0xFF3B82F6),
        onInfo = Color(0xFFFFFFFF),
        infoContainer = Color(0xFF173A7A),
        onInfoContainer = Color(0xFFDBE8FE),
    )
}
