package com.vplayed.core.geo.data

import com.vplayed.core.network.RedactBody
import com.vplayed.core.network.WithoutGeoToken
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path

/** Public IP lookup (third-party service; called with a client that sends no app headers). */
interface PublicIpApi {
    @GET("https://api.ipify.org/?format=json")
    suspend fun getIpAddress(): PublicIpDto
}

/** VPlayed geo-restriction service (main backend). */
interface GeoRestrictionApi {
    /** Must not carry the geo token it is fetching, and its body (the token) is never logged. */
    @WithoutGeoToken
    @RedactBody
    @GET("mediaview/api/v1/common/geo-data/{ip}")
    suspend fun getGeoRestrictionAccessKey(@Path("ip", encoded = true) ip: String): GeoAccessKeyResponseDto
}

@Serializable
data class PublicIpDto(val ip: String? = null)

@Serializable
data class GeoAccessKeyResponseDto(val response: GeoAccessKeyDto? = null)

@Serializable
data class GeoAccessKeyDto(@SerialName("geo_token") val geoToken: String? = null)

/** JSON inside the Base64 geo token. */
@Serializable
data class GeoLocationDto(val currency: String? = null)
