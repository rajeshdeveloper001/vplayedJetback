package com.vplayed.feature.dashboard.home.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

/*
 * Wire models for the home APIs. Only fields the app uses are declared (the JSON parser ignores
 * the rest). Fields whose JSON type varies between items are typed as JsonElement/JsonPrimitive
 * and normalised in HomeMappers.
 */

/** Standard VPlayed response envelope. */
@Serializable
data class ApiEnvelope<T>(
    val statusCode: Int? = null,
    val status: String? = null,
    val message: String? = null,
    val response: T? = null,
)

// ---- GET mediaview/api/v1/homebanners

@Serializable
data class BannerListDto(val data: List<BannerDto> = emptyList())

@Serializable
data class BannerDto(
    @SerialName("_id") val id: String,
    @SerialName("master_details") val content: BannerContentDto? = null,
)

@Serializable
data class BannerContentDto(
    @SerialName("_id") val id: String? = null,
    val title: String? = null,
    val slug: String? = null,
    val description: String? = null,
    /** Genre objects, or bare genre ids when the request carries app headers. */
    val genre: List<JsonElement>? = null,
    /** Primary genre; its `title` is a string or a localized object. */
    @SerialName("default_genre") val defaultGenre: JsonElement? = null,
    /** 1 = video, 2 = series, 6 = micro series. */
    @SerialName("sho_type") val showType: Int? = null,
    @SerialName("video_banner_images") val videoBannerImages: ImageSetDto? = null,
    @SerialName("new_thumbnail_images") val thumbnails: ImageSetDto? = null,
    /** Title artwork; `default` holds PNG `mobile`/`web` variants. */
    @SerialName("title_images") val titleImages: ImageSetDto? = null,
    /** Single title artwork path (may be AVIF). */
    @SerialName("title_image") val titleImage: String? = null,
)

// ---- GET mediaview/api/v1/home?page=&limit=&inside_limit=

@Serializable
data class HomePageDto(
    val data: List<SectionDto> = emptyList(),
    val total: Int = 0,
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val next: Int? = null,
)

@Serializable
data class SectionDto(
    @SerialName("_id") val id: String,
    /** A plain string on /home, a localized object (`{"en": "…"}`) on /home/all/{id}. */
    val title: JsonElement? = null,
    val type: Int = 0,
    @SerialName("is_top_ten") val isTopTen: Boolean = false,
    @SerialName("row_count") val rowCount: Int = 1,
    val total: Int = 0,
    val next: Int? = null,
    val data: List<ContentItemDto> = emptyList(),
)

// ---- GET mediaview/api/v1/home/all/{sectionId}?page=&inside_limit=

@Serializable
data class SectionItemsPageDto(
    val category: SectionDto? = null,
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val next: Int? = null,
)

// ---- Shared item model (videos, series, promos, audio, channels, bundles, live, micro series)

@Serializable
data class ContentItemDto(
    @SerialName("_id") val id: String,
    val title: String? = null,
    /** Channels and bundles use `name` instead of `title`. */
    val name: String? = null,
    val slug: String? = null,
    val type: Int? = null,
    /** 1 = video, 2 = series, 6 = micro series. */
    @SerialName("sho_type") val showType: Int? = null,
    val description: String? = null,
    /** Seconds; sent as a string or a number depending on the item. */
    @SerialName("video_duration") val videoDuration: JsonPrimitive? = null,
    /** Audio tracks: seconds as a string. */
    @SerialName("audio_duration") val audioDuration: JsonPrimitive? = null,
    @SerialName("episodes_count") val episodesCount: JsonPrimitive? = null,
    /** Objects (`{"title": "Action"}`), plain strings or bare ids, depending on the request. */
    val genre: List<JsonElement>? = null,
    @SerialName("default_genre") val defaultGenre: JsonElement? = null,
    val prices: List<PriceDto>? = null,
    @SerialName("new_thumbnail_images") val thumbnails: ImageSetDto? = null,
    @SerialName("thumbnail_image") val thumbnailImage: String? = null,
    @SerialName("video_banner_images") val videoBannerImages: ImageSetDto? = null,
    /** Absolute URL (bundles). */
    @SerialName("banner_img") val bannerImage: String? = null,
    @SerialName("bulk_thumbnail_images") val bundleThumbnails: ImageSetDto? = null,
    @SerialName("profile_picture") val profilePicture: String? = null,
    // Live streams: 1 = live, 0 = scheduled, 2 = finished; ISO-8601 UTC times.
    @SerialName("streaming_status") val streamingStatus: Int? = null,
    @SerialName("schedule_at") val scheduleAt: String? = null,
    @SerialName("stream_start_time") val streamStartTime: String? = null,
    @SerialName("studio_name") val studioName: String? = null,
    // Promos.
    @SerialName("view_count") val viewCount: JsonPrimitive? = null,
    @SerialName("guds_count") val likeCount: JsonPrimitive? = null,
    // Audio.
    @SerialName("artist_name") val artistName: String? = null,
    // Channels: brand color "#RRGGBB".
    @SerialName("bg_card_color") val cardColor: String? = null,
    @SerialName("follow_count") val followCount: JsonPrimitive? = null,
    // Bundles.
    /** Localized object, e.g. `{"en": "Special only for you"}`. */
    val label: JsonElement? = null,
    val days: JsonPrimitive? = null,
)

@Serializable
data class PriceDto(
    val amount: Double? = null,
    val currency: String? = null,
    @SerialName("currency_type") val currencyType: Int? = null,
    @SerialName("currency_sign") val currencySign: String? = null,
    @SerialName("is_active") val isActive: Boolean? = null,
    /** Bundles: amount taken off [amount] (0 = no discount) and the same as a percentage. */
    @SerialName("discounted_price") val discountedPrice: Double? = null,
    @SerialName("discounted_percentage") val discountedPercentage: Double? = null,
)

/**
 * Image variants. `default` maps size codes to relative paths: tl/tc/to = 1280×720,
 * tv = 889×500, tp = 420×236, tm = 300×169, ts/ta = thumbnails (all 16:9); banners use
 * `mobile` (840×385) and `web` (1920×880).
 */
@Serializable
data class ImageSetDto(val default: Map<String, String>? = null)
