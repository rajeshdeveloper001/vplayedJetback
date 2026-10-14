package com.vplayed.feature.dashboard.home.domain.model

/** Content type codes used by the home API (`type` on sections and items). */
enum class ContentType(val code: Int) {
    VIDEO(1),
    PROMO(2),
    AUDIO(3),
    CHANNEL(4),
    BUNDLE(5),
    LIVE(6),
    MICRO_SERIES(7),
    UNKNOWN(-1),
    ;

    companion object {
        fun fromCode(code: Int?): ContentType = entries.firstOrNull { it.code == code } ?: UNKNOWN
    }
}

/**
 * What a title is (`sho_type`): the hero chip shows this, never a genre. Matches the previous
 * app's mapping (1 = video, 2 = series, 6 = micro series).
 */
enum class ShowType(val code: Int) {
    VIDEO(1),
    SERIES(2),
    MICRO_SERIES(6),
    ;

    companion object {
        fun fromCode(code: Int?): ShowType? = entries.firstOrNull { it.code == code }
    }
}

/** Which prototype component a home row renders with. */
enum class SectionStyle {
    /** 16:9 card with title and "Genre • duration" (`.hcard`): videos and series. */
    LANDSCAPE,

    /** 3:4 poster with title and meta (`.pcard`): micro series. */
    PORTRAIT,

    /** 3:4 poster behind a large outlined rank numeral (`.t10`): `is_top_ten` rows. */
    TOP_TEN,

    /** Wide live card with badge, schedule, channel and action (`.lcard`). */
    LIVE,

    /** 16:9 card with a duration pill and views · likes (`.hcard.pstat`): promos. */
    PROMO,

    /** Vinyl disc with the cover as its label (`.mcard`): audio. */
    MUSIC,

    /** Brand-colored channel tile with logo, description, followers and Follow (`.chtile`). */
    CHANNEL,

    /** Bulk-purchase promo card (`.premium-in`). Rendered without a row title. */
    OFFER,
}

data class HomeBanner(
    val id: String,
    val title: String,
    val description: String?,
    /** Video / Series / Micro series, shown in the hero chip; null when the API sends no type. */
    val showType: ShowType?,
    val imageUrl: String?,
    /** Logo-style title artwork; the hero shows it instead of the title text when it loads. */
    val titleImageUrl: String?,
)

data class Price(val amount: Double, val currencySign: String, val currency: String)

data class ContentItem(
    val id: String,
    val title: String,
    val imageUrl: String?,
    val type: ContentType,
    /** First genre name (genre ids are dropped). */
    val genre: String? = null,
    /** Formatted running time, e.g. "2h 08m" or "4m 27s". */
    val duration: String? = null,
    /** Episode count for series and micro series (shown when there is no duration). */
    val episodeCount: Int? = null,
    val showType: ShowType? = null,
    val price: Price? = null,
    val details: ItemDetails = ItemDetails.None,
) {
    val isLive: Boolean get() = (details as? ItemDetails.Live)?.status == LiveStatus.LIVE
}

/** Data only some card types show. */
sealed interface ItemDetails {
    data object None : ItemDetails

    /** Live stream (`type` 6). [startsAtMillis] is the start time (UTC epoch millis). */
    data class Live(val status: LiveStatus, val startsAtMillis: Long?, val channelName: String?) : ItemDetails

    /** Promo clip (`type` 2). */
    data class Promo(val views: Long, val likes: Long) : ItemDetails

    /** Audio track (`type` 3). */
    data class Audio(val artist: String?) : ItemDetails

    /** Channel / studio (`type` 4). [brandColor] is ARGB from `bg_card_color`. */
    data class Channel(val brandColor: Long?, val description: String?, val followers: Int) : ItemDetails

    /** Bulk-purchase bundle (`type` 5). */
    data class Bundle(
        val label: String?,
        val description: String?,
        val days: Int?,
        /** Price to pay (after discount) and the original price when discounted. */
        val price: Price?,
        val originalPrice: Price?,
        val discountPercent: Int?,
        val accent: BundleAccent,
    ) : ItemDetails
}

/** `streaming_status`: 1 = live now, 0 = scheduled, anything else = finished. */
enum class LiveStatus { LIVE, UPCOMING, ENDED }

/** Prototype `.premium-in` color variants, chosen from the bundle's banner artwork color. */
enum class BundleAccent { BLUE, GOLD, VIOLET, GREEN }

data class HomeSection(
    val id: String,
    val title: String,
    val type: ContentType,
    val style: SectionStyle,
    /** 1 = single horizontal row, 2 = two stacked rows scrolling together. */
    val rowCount: Int,
    val items: List<ContentItem>,
    val totalItems: Int,
    /** Next page of this row's items, or null when every item is loaded. */
    val nextItemsPage: Int?,
) {
    val canLoadMoreItems: Boolean get() = nextItemsPage != null

    /** The previous app offered "view all" for rows with more than two items (not top 10 / bundles). */
    val hasViewAll: Boolean
        get() = totalItems > VIEW_ALL_MIN_ITEMS && style != SectionStyle.TOP_TEN && style != SectionStyle.OFFER

    private companion object {
        const val VIEW_ALL_MIN_ITEMS = 2
    }
}

/** One page of home rows. */
data class HomeSectionsPage(
    val sections: List<HomeSection>,
    val nextPage: Int?,
    val totalSections: Int,
)

/** One page of a single row's items. */
data class SectionItemsPage(
    val items: List<ContentItem>,
    val nextPage: Int?,
)
