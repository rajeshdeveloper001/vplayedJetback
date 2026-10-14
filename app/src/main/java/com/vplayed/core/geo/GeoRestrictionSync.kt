package com.vplayed.core.geo

import com.vplayed.core.dispatcher.ApplicationScope
import com.vplayed.core.geo.work.GeoRefreshScheduler
import com.vplayed.core.network.connectivity.NetworkMonitor
import com.vplayed.core.network.connectivity.NetworkStatus
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Keeps [GeoRestrictionStore] current: requests a refresh when the app starts online, when the
 * device comes back online, and when it switches to another network (the public IP changes).
 * While the connection stays the same nothing is requested.
 */
@Singleton
class GeoRestrictionSync @Inject constructor(
    private val networkMonitor: NetworkMonitor,
    private val scheduler: GeoRefreshScheduler,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val started = AtomicBoolean(false)

    /** Starts observing connectivity; later calls do nothing. */
    // debounce is still @FlowPreview in kotlinx.coroutines; it collapses connectivity flapping
    // (e.g. Wi-Fi ↔ mobile hand-over) into one refresh.
    @OptIn(FlowPreview::class)
    fun start() {
        if (!started.compareAndSet(false, true)) return
        scope.launch {
            networkMonitor.status
                .debounce(SETTLE_TIME)
                .distinctUntilChanged()
                .collect { status -> if (status is NetworkStatus.Online) scheduler.requestRefresh() }
        }
    }

    /** Explicit refresh, e.g. before a geo-restricted flow. */
    fun refreshNow() = scheduler.requestRefresh()

    private companion object {
        val SETTLE_TIME = 1.seconds
    }
}
