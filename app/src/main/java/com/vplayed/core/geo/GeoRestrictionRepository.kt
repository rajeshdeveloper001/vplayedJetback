package com.vplayed.core.geo

import com.vplayed.core.error.AppError
import com.vplayed.core.error.DataResult
import com.vplayed.core.error.resultOf
import com.vplayed.core.geo.data.GeoRestrictionApi
import com.vplayed.core.geo.data.PublicIpApi
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Result of one geo refresh. The store keeps its previous values for every outcome except [Success]. */
enum class GeoRefreshOutcome {
    Success,

    /** Network error, timeout or server error: worth retrying later. */
    TransientFailure,

    /** Bad IP, missing token or unexpected response: retrying now would not help. */
    InvalidResponse,
}

/**
 * Refreshes [GeoRestrictionStore]: public IP → geo access key for that IP → currency from the
 * token. Calls are serialized, so two triggers never run simultaneous requests. Never throws
 * (except cancellation) and never logs the IP or token.
 */
@Singleton
class GeoRestrictionRepository @Inject constructor(
    private val publicIpApi: PublicIpApi,
    private val geoRestrictionApi: GeoRestrictionApi,
    private val decoder: GeoTokenDecoder,
    private val store: GeoRestrictionStore,
) {
    private val mutex = Mutex()

    suspend fun refresh(): GeoRefreshOutcome = mutex.withLock { refreshLocked() }

    private suspend fun refreshLocked(): GeoRefreshOutcome {
        val ip = when (val result = resultOf { publicIpApi.getIpAddress() }) {
            is DataResult.Failure -> return result.error.toOutcome()
            is DataResult.Success -> result.data.ip?.trim()?.takeIf(::isValidIp)
                ?: return GeoRefreshOutcome.InvalidResponse
        }
        store.updateIp(ip)

        val token = when (val result = resultOf { geoRestrictionApi.getGeoRestrictionAccessKey(ip) }) {
            is DataResult.Failure -> return result.error.toOutcome()
            is DataResult.Success -> result.data.response?.geoToken?.trim()?.takeIf { it.isNotEmpty() }
                ?: return GeoRefreshOutcome.InvalidResponse
        }
        store.updateGeo(token, decoder.decodeCurrency(token))
        return GeoRefreshOutcome.Success
    }

    private fun AppError.toOutcome(): GeoRefreshOutcome = when (this) {
        AppError.Network -> GeoRefreshOutcome.TransientFailure
        is AppError.Server -> if (code >= SERVER_ERROR || code in RETRYABLE_CLIENT_CODES) {
            GeoRefreshOutcome.TransientFailure
        } else {
            GeoRefreshOutcome.InvalidResponse
        }
        else -> GeoRefreshOutcome.InvalidResponse
    }

    private companion object {
        const val SERVER_ERROR = 500
        val RETRYABLE_CLIENT_CODES = setOf(408, 429)

        // IPv4 or IPv6 text (the IP is used as a URL path segment, so only these characters pass).
        val IP_PATTERN = Regex("^[0-9A-Fa-f:.]{2,45}$")

        fun isValidIp(ip: String): Boolean = IP_PATTERN.matches(ip) && (ip.contains('.') || ip.contains(':'))
    }
}
