package com.vplayed.core.network.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/** Connectivity of the default network. */
sealed interface NetworkStatus {
    data object Offline : NetworkStatus

    /** [networkHandle] changes when the device switches networks (e.g. Wi-Fi → mobile data). */
    data class Online(val networkHandle: Long) : NetworkStatus
}

interface NetworkMonitor {
    /** Current status first, then every change. */
    val status: Flow<NetworkStatus>
}

/** [NetworkMonitor] backed by [ConnectivityManager.NetworkCallback] on the default network. */
@Singleton
class ConnectivityNetworkMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
) : NetworkMonitor {

    override val status: Flow<NetworkStatus> = callbackFlow {
        val connectivityManager = context.getSystemService<ConnectivityManager>()
        if (connectivityManager == null) {
            trySend(NetworkStatus.Offline)
            awaitClose()
            return@callbackFlow
        }
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(NetworkStatus.Online(network.networkHandle))
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                trySend(capabilities.toStatus(network))
            }

            override fun onLost(network: Network) {
                trySend(NetworkStatus.Offline)
            }
        }
        trySend(connectivityManager.currentStatus())
        val registered = try {
            connectivityManager.registerDefaultNetworkCallback(callback)
            true
        } catch (_: RuntimeException) {
            // TooManyRequestsException / SecurityException: keep the initial status only.
            false
        }
        awaitClose { if (registered) connectivityManager.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()

    private fun ConnectivityManager.currentStatus(): NetworkStatus {
        val network = activeNetwork ?: return NetworkStatus.Offline
        val capabilities = getNetworkCapabilities(network) ?: return NetworkStatus.Offline
        return capabilities.toStatus(network)
    }

    private fun NetworkCapabilities.toStatus(network: Network): NetworkStatus =
        if (hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            NetworkStatus.Online(network.networkHandle)
        } else {
            NetworkStatus.Offline
        }
}
