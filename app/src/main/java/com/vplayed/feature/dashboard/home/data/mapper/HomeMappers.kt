package com.vplayed.feature.dashboard.home.data.mapper

import com.vplayed.feature.dashboard.home.data.remote.dto.BannerDto
import com.vplayed.feature.dashboard.home.data.remote.dto.ContentItemDto
import com.vplayed.feature.dashboard.home.data.remote.dto.HomePageDto
import com.vplayed.feature.dashboard.home.data.remote.dto.ImageSetDto
import com.vplayed.feature.dashboard.home.data.remote.dto.PriceDto
import com.vplayed.feature.dashboard.home.data.remote.dto.SectionDto
import com.vplayed.feature.dashboard.home.data.remote.dto.SectionItemsPageDto
import com.vplayed.feature.dashboard.home.domain.model.BundleAccent
import com.vplayed.feature.dashboard.home.domain.model.ContentItem
import com.vplayed.feature.dashboard.home.domain.model.ContentType
import com.vplayed.feature.dashboard.home.domain.model.HomeBanner
import com.vplayed.feature.dashboard.home.domain.model.HomeSection
import com.vplayed.feature.dashboard.home.domain.model.HomeSectionsPage
import com.vplayed.feature.dashboard.home.domain.model.ItemDetails
import com.vplayed.feature.dashboard.home.domain.model.LiveStatus
import com.vplayed.feature.dashboard.home.domain.model.Price
import com.vplayed.feature.dashboard.home.domain.model.SectionItemsPage
import com.vplayed.feature.dashboard.home.domain.model.SectionStyle
import com.vplayed.feature.dashboard.home.domain.model.ShowType
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

/**
 * Converts home API models to domain models. Pure Kotlin; [imageBaseUrl] turns the API's relative
 * image paths into absolute URLs. Image preferences mirror the previous app (Utils.imageURL).
 */
class HomeMapper(private val imageBaseUrl: String) {

    fun banner(dto: BannerDto): HomeBanner? {
        val content = dto.content ?: return null
        val title = content.title?.takeIf { it.isNotBlank() } ?: return null
        return HomeBanner(
            id = content.id ?: dto.id,
            title = title,
            description = content.description?.takeIf { it.isNotBlank() },
            showType = ShowType.fromCode(content.showType),
            imageUrl = imageUrl(
                content.videoBannerImages.variant("mobile", "web")
                    ?: content.thumbnails.variant("tl", "tc", "tv"),
            ),
            // PNG variants first: `title_image` alone may be AVIF, which Android decodes only from API 31.
            titleImageUrl = imageUrl(content.titleImages.variant("mobile", "web") ?: content.titleImage),
        )
    }

    /** Maps one page of rows, dropping rows without items (the previous app hid them too). */
    fun sectionsPage(dto: HomePageDto): HomeSectionsPage = HomeSectionsPage(
        sections = dto.data.mapNotNull(::section),
        nextPage = dto.next,
        totalSections = dto.total,
    )

    fun section(dto: SectionDto): HomeSection? {
        val type = ContentType.fromCode(dto.type)
        val style = sectionStyle(dto)
        val items = dto.data.map { item(it, sectionType = type, style = style) }
        if (items.isEmpty()) return null
        return HomeSection(
            id = dto.id,
            title = dto.title.localizedText().orEmpty(),
            type = type,
            style = style,
            rowCount = dto.rowCount.coerceIn(1, MAX_ROWS),
            items = items,
            totalItems = dto.total,
            nextItemsPage = dto.next,
        )
    }

    fun sectionItemsPage(dto: SectionItemsPageDto): SectionItemsPage {
        val category = dto.category
        val type = ContentType.fromCode(category?.type)
        val style = category?.let(::sectionStyle) ?: SectionStyle.LANDSCAPE
        return SectionItemsPage(
            items = category?.data.orEmpty().map { item(it, sectionType = type, style = style) },
            nextPage = dto.next,
        )
    }

    /** The prototype component for each row type (see docs/api/home-api.md › Section types). */
    private fun sectionStyle(dto: SectionDto): SectionStyle = when {
        dto.isTopTen -> SectionStyle.TOP_TEN
        else -> when (ContentType.fromCode(dto.type)) {
            ContentType.MICRO_SERIES -> SectionStyle.PORTRAIT
            ContentType.LIVE -> SectionStyle.LIVE
            ContentType.PROMO -> SectionStyle.PROMO
            ContentType.AUDIO -> SectionStyle.MUSIC
            ContentType.CHANNEL -> SectionStyle.CHANNEL
            ContentType.BUNDLE -> SectionStyle.OFFER
            ContentType.VIDEO, ContentType.UNKNOWN -> SectionStyle.LANDSCAPE
        }
    }

    fun item(
        dto: ContentItemDto,
        sectionType: ContentType = ContentType.UNKNOWN,
        style: SectionStyle = SectionStyle.LANDSCAPE,
    ): ContentItem {
        val type = ContentType.fromCode(dto.type).takeUnless { it == ContentType.UNKNOWN } ?: sectionType
        val portrait = style == SectionStyle.PORTRAIT || style == SectionStyle.TOP_TEN
        val seconds = (if (type == ContentType.AUDIO) dto.audioDuration else dto.videoDuration)?.content?.toDoubleOrNull()
        return ContentItem(
            id = dto.id,
            title = (dto.title ?: dto.name).orEmpty(),
            imageUrl = imageUrl(if (portrait) dto.thumbnailImage ?: imagePath(dto, type) else imagePath(dto, type)),
            type = type,
            genre = primaryGenre(dto.defaultGenre, dto.genre),
            duration = seconds?.let(::formatDuration),
            episodeCount = dto.episodesCount.intValue()?.takeIf { it > 0 },
            showType = ShowType.fromCode(dto.showType),
            price = dto.prices.preferredPrice()?.let { it.toPrice(it.amount ?: 0.0) },
            details = details(dto, type),
        )
    }

    private fun details(dto: ContentItemDto, type: ContentType): ItemDetails = when (type) {
        ContentType.LIVE -> {
            val status = when (dto.streamingStatus) {
                STREAMING_LIVE -> LiveStatus.LIVE
                STREAMING_SCHEDULED -> LiveStatus.UPCOMING
                else -> LiveStatus.ENDED
            }
            val start = if (status == LiveStatus.UPCOMING) dto.scheduleAt ?: dto.streamStartTime else dto.streamStartTime ?: dto.scheduleAt
            ItemDetails.Live(status = status, startsAtMillis = parseIsoMillis(start), channelName = dto.studioName?.takeIf { it.isNotBlank() })
        }
        ContentType.PROMO -> ItemDetails.Promo(views = dto.viewCount.longValue() ?: 0, likes = dto.likeCount.longValue() ?: 0)
        ContentType.AUDIO -> ItemDetails.Audio(artist = dto.artistName?.takeIf { it.isNotBlank() })
        ContentType.CHANNEL -> ItemDetails.Channel(
            brandColor = parseHexColor(dto.cardColor),
            description = dto.description?.takeIf { it.isNotBlank() },
            followers = dto.followCount.intValue() ?: 0,
        )
        ContentType.BUNDLE -> bundle(dto)
        else -> ItemDetails.None
    }

    /** `discounted_price` is the amount taken off `amount`; the card shows the result and strikes the original. */
    private fun bundle(dto: ContentItemDto): ItemDetails.Bundle {
        val price = dto.prices.preferredPrice()
        val amount = price?.amount ?: 0.0
        val discount = price?.discountedPrice?.takeIf { it > 0 && it < amount }
        return ItemDetails.Bundle(
            label = dto.label.localizedText(),
            description = dto.description?.takeIf { it.isNotBlank() },
            days = dto.days.intValue()?.takeIf { it > 0 },
            price = price?.toPrice(if (discount != null) amount - discount else amount),
            originalPrice = if (discount != null) price?.toPrice(amount) else null,
            discountPercent = if (discount != null) {
                price?.discountedPercentage?.roundToInt()?.takeIf { it > 0 } ?: (discount / amount * PERCENT).roundToInt()
            } else {
                null
            },
            accent = bundleAccent(dto.bannerImage),
        )
    }

    /** The API's stock banners are `blue.png`, `yellow.png`, `dark-red.png`; custom artwork has no color hint. */
    private fun bundleAccent(bannerImage: String?): BundleAccent {
        val name = bannerImage.orEmpty().substringAfterLast('/').lowercase(Locale.ROOT)
        return when {
            "blue" in name -> BundleAccent.BLUE
            "yellow" in name || "gold" in name -> BundleAccent.GOLD
            "green" in name -> BundleAccent.GREEN
            else -> BundleAccent.VIOLET
        }
    }

    private fun imagePath(dto: ContentItemDto, type: ContentType): String? = when (type) {
        ContentType.CHANNEL -> dto.profilePicture
        ContentType.BUNDLE -> dto.bannerImage ?: dto.bundleThumbnails.variant("tl", "tv", "tp")
        ContentType.MICRO_SERIES -> dto.videoBannerImages.variant("mobile", "web") ?: dto.thumbnails.cardVariant()
        else -> dto.thumbnails.cardVariant() ?: dto.thumbnailImage ?: dto.videoBannerImages.variant("mobile", "web")
    }

    /** Absolute URL for an API image path; absolute URLs pass through unchanged. */
    fun imageUrl(path: String?): String? {
        val value = path?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return if (value.startsWith("http://") || value.startsWith("https://")) value
        else imageBaseUrl + value.trimStart('/')
    }

    private companion object {
        const val MAX_ROWS = 2
        const val STREAMING_SCHEDULED = 0
        const val STREAMING_LIVE = 1
        const val INR_CURRENCY_TYPE = 1
        const val SECONDS_PER_MINUTE = 60
        const val MINUTES_PER_HOUR = 60
        const val PERCENT = 100
        const val OPAQUE = 0xFF000000L
        const val RGB_HEX_LENGTH = 6
        const val ARGB_HEX_LENGTH = 8
        const val HEX_RADIX = 16

        /** Card thumbnails: a mid-size 16:9 variant first (sharp on phones, light on data). */
        fun ImageSetDto?.cardVariant(): String? = variant("tv", "tc", "tl", "tp", "tm")

        fun ImageSetDto?.variant(vararg keys: String): String? {
            val images = this?.default ?: return null
            return keys.firstNotNullOfOrNull { key -> images[key]?.takeIf { it.isNotBlank() } }
        }

        /** Prefers the INR price (currency_type 1), then the first active price. */
        fun List<PriceDto>?.preferredPrice(): PriceDto? {
            val active = this.orEmpty().filter { it.isActive != false && it.amount != null }
            return active.firstOrNull { it.currencyType == INR_CURRENCY_TYPE } ?: active.firstOrNull()
        }

        fun PriceDto.toPrice(value: Double) = Price(value, currencySign ?: currency.orEmpty(), currency.orEmpty())

        fun JsonPrimitive?.longValue(): Long? = this?.content?.toDoubleOrNull()?.toLong()

        fun JsonPrimitive?.intValue(): Int? = longValue()?.toInt()

        /** "2h 08m", "4m 27s" or "45s" (prototype card meta and promo duration pill). */
        fun formatDuration(seconds: Double): String? {
            if (seconds < 1) return null
            val total = seconds.toLong()
            val hours = total / (SECONDS_PER_MINUTE * MINUTES_PER_HOUR)
            val minutes = total / SECONDS_PER_MINUTE % MINUTES_PER_HOUR
            val secs = total % SECONDS_PER_MINUTE
            return when {
                hours > 0 -> "${hours}h %02dm".format(Locale.ROOT, minutes)
                minutes > 0 -> "${minutes}m %02ds".format(Locale.ROOT, secs)
                else -> "${secs}s"
            }
        }

        /** "#RRGGBB" or "#AARRGGBB" to an ARGB value; null when missing or malformed. */
        fun parseHexColor(value: String?): Long? {
            val hex = value?.trim()?.removePrefix("#") ?: return null
            val parsed = hex.toLongOrNull(HEX_RADIX) ?: return null
            return when (hex.length) {
                RGB_HEX_LENGTH -> OPAQUE or parsed
                ARGB_HEX_LENGTH -> parsed
                else -> null
            }
        }

        /** ISO-8601 UTC timestamps as sent by the API ("2026-10-12T20:45:00.000Z"), with or without millis. */
        fun parseIsoMillis(value: String?): Long? {
            val text = value?.takeIf { it.isNotBlank() } ?: return null
            return listOf("yyyy-MM-dd'T'HH:mm:ss.SSSX", "yyyy-MM-dd'T'HH:mm:ssX").firstNotNullOfOrNull { pattern ->
                try {
                    SimpleDateFormat(pattern, Locale.ROOT).apply { timeZone = TimeZone.getTimeZone("UTC") }.parse(text)?.time
                } catch (_: ParseException) {
                    null
                }
            }
        }

        /** Database ids (24 hex chars) sometimes sent in place of genre names. */
        private val ObjectId = Regex("^[0-9a-fA-F]{24}$")

        /** Genre names from `[{"title": "Action"}]`, `[{"title": {"en": "Action"}}]` or `["Action"]`; ids are skipped. */
        fun List<JsonElement>?.genreNames(): List<String> = orEmpty().mapNotNull { it.genreName() }

        /**
         * The item's main genre for the card meta line. `default_genre` is an object (its title is
         * sometimes lowercase, e.g. "action"), a lowercase slug or an id; it is resolved to the
         * display title of the matching entry in `genre` when there is one.
         */
        fun primaryGenre(defaultGenre: JsonElement?, genres: List<JsonElement>?): String? {
            val default = defaultGenre.genreName() ?: return genres.genreNames().firstOrNull()
            val match = genres.orEmpty().filterIsInstance<JsonObject>().firstOrNull { genre ->
                listOf(genre["slug"], genre["title"]).any { (it as? JsonPrimitive)?.content.equals(default, ignoreCase = true) }
            }
            return match.genreName()
                ?: if (defaultGenre is JsonPrimitive) default.replaceFirstChar { it.titlecase(Locale.ROOT) } else default
        }

        fun JsonElement?.genreName(): String? = when (this) {
            is JsonPrimitive -> content.takeIf { isString && !ObjectId.matches(content) }
            is JsonObject -> this["title"].localizedText()
            else -> null
        }?.takeIf { it.isNotBlank() }

        /** Reads a plain string or a localized object (`{"en": "…"}`, falling back to any value). */
        fun JsonElement?.localizedText(): String? = when (this) {
            is JsonPrimitive -> content.takeIf { isString && it.isNotBlank() }
            is JsonObject -> ((this["en"] ?: values.firstOrNull()) as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() }
            else -> null
        }
    }
}
