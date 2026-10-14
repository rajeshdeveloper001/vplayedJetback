package com.vplayed.core.geo

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Geo-restriction values the backend needs. [geoToken] is a credential: never log it. */
data class GeoRestrictionState(
    /** Public IP of the current network, or null before the first successful lookup. */
    val ip: String? = null,
    /** Geo access key from `common/geo-data/{ip}`, sent as `x-bearer-token`; null until fetched. */
    val geoToken: String? = null,
    /** Currency decoded from [geoToken]; [DEFAULT_CURRENCY] until a valid one is known. */
    val currency: String = DEFAULT_CURRENCY,
) {
    override fun toString(): String =
        "GeoRestrictionState(ip=${if (ip == null) "none" else "set"}, " +
            "geoToken=${if (geoToken == null) "none" else "set"}, currency=$currency)"

    companion object {
        const val DEFAULT_CURRENCY = "INR"
    }
}

/**
 * Single source of truth for the public IP, geo token and currency (replaces the previous app's
 * `NetworkChangeWorker` companion fields and `AppController.ip`). Kept in memory only, as before.
 *
 * Everyone reads [state]; only [GeoRestrictionRepository] writes. Updates are atomic and ignore
 * blank values, so a failed or empty response never erases the last valid values.
 */
@Singleton
class GeoRestrictionStore @Inject constructor() {

    private val mutableState = MutableStateFlow(GeoRestrictionState())
    val state: StateFlow<GeoRestrictionState> = mutableState.asStateFlow()

    fun updateIp(ip: String) {
        if (ip.isBlank()) return
        mutableState.update { it.copy(ip = ip) }
    }

    /** Saves a new [token]; the currency changes only when [currency] is a non-blank value. */
    fun updateGeo(token: String, currency: String?) {
        if (token.isBlank()) return
        mutableState.update { current ->
            current.copy(
                geoToken = token,
                currency = currency?.takeIf { it.isNotBlank() } ?: current.currency,
            )
        }
    }
}
