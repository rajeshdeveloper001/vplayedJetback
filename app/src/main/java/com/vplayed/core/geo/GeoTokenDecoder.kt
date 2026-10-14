package com.vplayed.core.geo

import com.vplayed.core.geo.data.GeoLocationDto
import javax.inject.Inject
import kotlin.io.encoding.Base64
import kotlinx.serialization.json.Json

/**
 * Reads the currency from a geo token (Base64 of `{"currency": "…"}`). Any invalid input yields
 * null instead of an exception, so a bad token can never crash the app.
 */
class GeoTokenDecoder @Inject constructor(private val json: Json) {

    fun decodeCurrency(token: String): String? {
        val compact = token.filterNot(Char::isWhitespace)
        if (compact.isEmpty()) return null
        return try {
            val decoded = base64.decode(compact).decodeToString()
            json.decodeFromString<GeoLocationDto>(decoded).currency?.trim()?.takeIf { it.isNotEmpty() }
        } catch (_: IllegalArgumentException) {
            // Not valid Base64, or the decoded text is not the expected JSON
            // (kotlinx SerializationException is an IllegalArgumentException).
            null
        }
    }

    private companion object {
        // Matches android.util.Base64.DEFAULT used by the previous app: standard alphabet, padding optional.
        val base64 = Base64.Default.withPadding(Base64.PaddingOption.PRESENT_OPTIONAL)
    }
}
