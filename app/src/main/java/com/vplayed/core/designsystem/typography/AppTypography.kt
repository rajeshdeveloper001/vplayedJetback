package com.vplayed.core.designsystem.typography

import androidx.annotation.StringRes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.vplayed.R
import com.vplayed.core.designsystem.dimension.WindowWidthClass

/**
 * A selectable font family. To add a brand font, put its .ttf files in `res/font/` (lowercase
 * names), declare a [FontFamily] like [AppFonts.InterFamily], and add an entry to [AppFonts.all].
 */
@Immutable
data class AppFont(
    val id: String,
    @param:StringRes val nameRes: Int,
    val fontFamily: FontFamily,
)

object AppFonts {
    /** Inter 4.1 (SIL OFL 1.1, see docs/licenses). Static instances so weights work on API 24+. */
    val InterFamily = FontFamily(
        Font(R.font.inter_light, FontWeight.Light),
        Font(R.font.inter_regular, FontWeight.Normal),
        Font(R.font.inter_italic, FontWeight.Normal, FontStyle.Italic),
        Font(R.font.inter_medium, FontWeight.Medium),
        Font(R.font.inter_semi_bold, FontWeight.SemiBold),
        Font(R.font.inter_bold, FontWeight.Bold),
        Font(R.font.inter_extra_bold, FontWeight.ExtraBold),
        Font(R.font.inter_black, FontWeight.Black),
    )

    val Inter = AppFont("inter", R.string.font_inter, InterFamily)

    /** Inter is the only bundled font. Add entries here to offer more (the settings picker appears automatically). */
    val all: List<AppFont> = listOf(Inter)
}

/**
 * Size, line height and letter spacing in sp. [fontFeatureSettings] uses CSS syntax,
 * e.g. "tnum" for tabular (fixed-width) numbers.
 */
@Immutable
data class TypeToken(
    val fontSize: Float,
    val lineHeight: Float,
    val letterSpacing: Float,
    val fontWeight: FontWeight,
    val fontFeatureSettings: String? = null,
)

/** Material 3 type scale. Material components (buttons, dialogs, text fields) read these. */
object TypeScaleTokens {
    val displayLarge = TypeToken(57f, 64f, -0.25f, FontWeight.Normal)
    val displayMedium = TypeToken(45f, 52f, 0f, FontWeight.Normal)
    val displaySmall = TypeToken(36f, 44f, 0f, FontWeight.Normal)
    val headlineLarge = TypeToken(32f, 40f, 0f, FontWeight.SemiBold)
    val headlineMedium = TypeToken(28f, 36f, 0f, FontWeight.SemiBold)
    val headlineSmall = TypeToken(24f, 32f, 0f, FontWeight.SemiBold)
    val titleLarge = TypeToken(22f, 28f, 0f, FontWeight.SemiBold)
    val titleMedium = TypeToken(16f, 24f, 0.15f, FontWeight.SemiBold)
    val titleSmall = TypeToken(14f, 20f, 0.1f, FontWeight.Medium)
    val bodyLarge = TypeToken(16f, 24f, 0.5f, FontWeight.Normal)
    val bodyMedium = TypeToken(14f, 20f, 0.25f, FontWeight.Normal)
    val bodySmall = TypeToken(12f, 16f, 0.4f, FontWeight.Normal)
    val labelLarge = TypeToken(14f, 20f, 0.1f, FontWeight.Medium)
    val labelMedium = TypeToken(12f, 16f, 0.5f, FontWeight.Medium)
    val labelSmall = TypeToken(11f, 16f, 0.5f, FontWeight.Medium)
}

/**
 * OTT-specific text roles, sized for phones. Roles marked in [OttTypeTokens.headerRoles] grow on
 * wider windows (see [headerScaleFor]); everything else keeps its size so lists stay dense.
 */
object OttTypeTokens {
    // Values are the prototype's computed styles on a 427dp phone (Inter).
    /** Hero title (prototype `.hero h1`); scales with window width, see `HeroBannerCarousel`. */
    val heroTitle = TypeToken(40f, 39.2f, -1.4f, FontWeight.Bold)
    /** Hero synopsis (prototype `.hero .syn`). */
    val heroSubtitle = TypeToken(14f, 22.4f, 0f, FontWeight.Normal)
    /** Page title, e.g. "Movies", "My List". */
    val screenHeader = TypeToken(28f, 34f, -0.25f, FontWeight.Bold)
    /** Row title (prototype `.row-head h2`); scales with window width, see `SectionRow`. */
    val header = TypeToken(21f, 26f, -0.21f, FontWeight.Bold)
    /** Secondary heading, or a detail-page section such as "Episodes". */
    val subHeader = TypeToken(16f, 22f, 0.1f, FontWeight.SemiBold)
    /** "See all" / "More" link next to a rail header. */
    val sectionLink = TypeToken(14f, 20f, 0.1f, FontWeight.SemiBold)
    /** Card headline: channel name, bulk-purchase offer title (prototype 20px bold). */
    val cardHeadline = TypeToken(20f, 24f, -0.2f, FontWeight.Bold)
    /** Title under a card or poster (prototype `.hinfo .t`, `.info .t`). */
    val cardTitle = TypeToken(14f, 17.5f, 0f, FontWeight.SemiBold)
    /** Body copy inside promo cards (prototype `.premium-in p`). */
    val cardBody = TypeToken(13f, 19.5f, 0f, FontWeight.Normal)
    /** Meta line under a card title (prototype `.hinfo .s`). */
    val cardSubtitle = TypeToken(12f, 15f, 0f, FontWeight.Normal)
    /** Detail-page metadata row: year · rating · duration · language. */
    val metadata = TypeToken(13f, 18f, 0.2f, FontWeight.Medium)
    /** Description / synopsis paragraphs. */
    val synopsis = TypeToken(15f, 22f, 0.15f, FontWeight.Normal)
    /** Chips and overlines (prototype `.badge-new`, `.pover`): uppercase, 0.14em tracking. */
    val badge = TypeToken(11f, 14f, 1.54f, FontWeight.Bold)
    /** Artwork tags (prototype `.lbadge` LIVE / UPCOMING): uppercase, 0.06em tracking. */
    val tag = TypeToken(12f, 15f, 0.72f, FontWeight.Bold)
    /** Primary call-to-action labels (prototype hero buttons). */
    val action = TypeToken(16f, 16f, 0f, FontWeight.Bold)
    /** Compact button labels (prototype `.lbtn`, `.premium-in .btn`). */
    val actionSmall = TypeToken(14f, 14f, 0f, FontWeight.Bold)
    /** Outlined/glass button labels (prototype channel "Follow"). */
    val actionMedium = TypeToken(15f, 18f, 0f, FontWeight.SemiBold)
    /** Initials inside logo wells (prototype `.clogo` monogram). */
    val monogram = TypeToken(17f, 17f, 0f, FontWeight.ExtraBold)
    /** Small helper text, credits, legal lines. */
    val caption = TypeToken(12f, 16f, 0.3f, FontWeight.Normal)
    /** Title in the player overlay. */
    val playerTitle = TypeToken(18f, 24f, 0f, FontWeight.SemiBold)
    /** Player position/duration; tabular numbers so digits do not jitter while playing. */
    val playerTimestamp = TypeToken(12f, 16f, 0.2f, FontWeight.Medium, fontFeatureSettings = "tnum")
    /** Top-10 rank numerals (prototype SVG: Inter 800, drawn outlined); sized in dp by `TopTenCard`. */
    val rank = TypeToken(224f, 224f, -13.4f, FontWeight.ExtraBold)

    // Forms (prototype sign-in card, 375–427dp phone values; formTitle and promoTitle grow on wide windows).
    /** Form heading (prototype `.auth-head h1`: 24px bold, -0.03em, line-height 1.15). */
    val formTitle = TypeToken(24f, 27.6f, -0.72f, FontWeight.Bold)
    /** Field label and small emphasized text (prototype `.field label`: 12px semibold). */
    val formLabel = TypeToken(12f, 16f, -0.06f, FontWeight.SemiBold)
    /** Text typed into a field (prototype input: 15px semibold). */
    val formInput = TypeToken(15f, 20f, -0.075f, FontWeight.SemiBold)
    /** Help, error and legal lines under a form (prototype `.help`, `.terms`: 12px, line-height 1.55). */
    val formSupporting = TypeToken(12f, 18.6f, 0f, FontWeight.Medium)
    /** Full-width form button label (prototype `.btn-primary`, `.social-btn`: 15px bold). */
    val formAction = TypeToken(15f, 20f, -0.075f, FontWeight.Bold)
    /** Divider text (prototype "or continue with email": 11px semibold, 0.08em). */
    val formDivider = TypeToken(11f, 14f, 0.88f, FontWeight.SemiBold)
    /** One digit of a verification code (prototype `.otp-row input`: 20–24px bold; fixed here). */
    val formCode = TypeToken(24f, 28f, 0f, FontWeight.Bold)
    /** Marketing headline beside a form (prototype `.left-heading h2`: 28→36px bold, -0.035em, 1.08). */
    val promoTitle = TypeToken(28f, 30.24f, -0.98f, FontWeight.Bold)
    /** Marketing body copy (prototype `.left-heading p`, `.feature h5`: 15px medium, 1.55). */
    val promoBody = TypeToken(15f, 23.25f, 0f, FontWeight.Medium)
}

/** OTT text styles, read via `AppDesignSystem.textStyles`. */
@Immutable
data class AppTextStyles(
    val heroTitle: TextStyle,
    val heroSubtitle: TextStyle,
    val screenHeader: TextStyle,
    val header: TextStyle,
    val subHeader: TextStyle,
    val sectionLink: TextStyle,
    val cardHeadline: TextStyle,
    val cardTitle: TextStyle,
    val cardBody: TextStyle,
    val cardSubtitle: TextStyle,
    val metadata: TextStyle,
    val synopsis: TextStyle,
    val badge: TextStyle,
    val tag: TextStyle,
    val action: TextStyle,
    val actionSmall: TextStyle,
    val actionMedium: TextStyle,
    val monogram: TextStyle,
    val caption: TextStyle,
    val playerTitle: TextStyle,
    val playerTimestamp: TextStyle,
    val rank: TextStyle,
    val formTitle: TextStyle,
    val formLabel: TextStyle,
    val formInput: TextStyle,
    val formSupporting: TextStyle,
    val formAction: TextStyle,
    val formDivider: TextStyle,
    val formCode: TextStyle,
    val promoTitle: TextStyle,
    val promoBody: TextStyle,
)

/** Accessible bounds for the in-app text scale. It multiplies the user's system font scale. */
object TextScale {
    const val MIN = 0.85f
    const val MAX = 1.3f
    const val DEFAULT = 1f

    fun coerce(value: Float): Float = value.coerceIn(MIN, MAX)
}

/** Extra scale for header roles on wider windows (tablet, landscape, desktop). */
fun headerScaleFor(widthClass: WindowWidthClass): Float = when (widthClass) {
    WindowWidthClass.COMPACT -> 1f
    WindowWidthClass.MEDIUM -> 1.15f
    WindowWidthClass.EXPANDED -> 1.3f
}

private fun TypeToken.toStyle(fontFamily: FontFamily, scale: Float) = TextStyle(
    fontFamily = fontFamily,
    fontWeight = fontWeight,
    fontSize = (fontSize * scale).sp,
    lineHeight = (lineHeight * scale).sp,
    letterSpacing = letterSpacing.sp,
    fontFeatureSettings = fontFeatureSettings,
)

/** Builds the Material [Typography] for [fontFamily], scaled by [textScale] (clamped to [TextScale]). */
fun appTypography(fontFamily: FontFamily, textScale: Float = TextScale.DEFAULT): Typography {
    val scale = TextScale.coerce(textScale)
    fun TypeToken.style() = toStyle(fontFamily, scale)
    return with(TypeScaleTokens) {
        Typography(
            displayLarge = displayLarge.style(),
            displayMedium = displayMedium.style(),
            displaySmall = displaySmall.style(),
            headlineLarge = headlineLarge.style(),
            headlineMedium = headlineMedium.style(),
            headlineSmall = headlineSmall.style(),
            titleLarge = titleLarge.style(),
            titleMedium = titleMedium.style(),
            titleSmall = titleSmall.style(),
            bodyLarge = bodyLarge.style(),
            bodyMedium = bodyMedium.style(),
            bodySmall = bodySmall.style(),
            labelLarge = labelLarge.style(),
            labelMedium = labelMedium.style(),
            labelSmall = labelSmall.style(),
        )
    }
}

/** Builds the OTT [AppTextStyles]; header roles additionally scale with [widthClass]. */
fun appTextStyles(
    fontFamily: FontFamily,
    textScale: Float = TextScale.DEFAULT,
    widthClass: WindowWidthClass = WindowWidthClass.COMPACT,
): AppTextStyles {
    val scale = TextScale.coerce(textScale)
    val headerScale = scale * headerScaleFor(widthClass)
    fun TypeToken.body() = toStyle(fontFamily, scale)
    fun TypeToken.heading() = toStyle(fontFamily, headerScale)
    return with(OttTypeTokens) {
        AppTextStyles(
            heroTitle = heroTitle.heading(),
            heroSubtitle = heroSubtitle.heading(),
            screenHeader = screenHeader.heading(),
            header = header.heading(),
            subHeader = subHeader.heading(),
            sectionLink = sectionLink.body(),
            cardHeadline = cardHeadline.body(),
            cardTitle = cardTitle.body(),
            cardBody = cardBody.body(),
            cardSubtitle = cardSubtitle.body(),
            metadata = metadata.body(),
            synopsis = synopsis.body(),
            badge = badge.body(),
            tag = tag.body(),
            action = action.body(),
            actionSmall = actionSmall.body(),
            actionMedium = actionMedium.body(),
            monogram = monogram.body(),
            caption = caption.body(),
            playerTitle = playerTitle.body(),
            playerTimestamp = playerTimestamp.body(),
            rank = rank.body(),
            formTitle = formTitle.heading(),
            formLabel = formLabel.body(),
            formInput = formInput.body(),
            formSupporting = formSupporting.body(),
            formAction = formAction.body(),
            formDivider = formDivider.body(),
            formCode = formCode.body(),
            promoTitle = promoTitle.heading(),
            promoBody = promoBody.body(),
        )
    }
}
