package com.vplayed.core.geo

import com.vplayed.core.geo.work.GeoRefreshScheduler
import com.vplayed.core.network.connectivity.NetworkMonitor
import com.vplayed.core.network.connectivity.NetworkStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

// advanceTimeBy/runCurrent drive the 1 s debounce on virtual time.
@OptIn(ExperimentalCoroutinesApi::class)
class GeoRestrictionSyncTest {

    private val networkEvents = MutableSharedFlow<NetworkStatus>(replay = 1)
    private val monitor = object : NetworkMonitor {
        override val status = networkEvents
    }
    private val scheduler = object : GeoRefreshScheduler {
        var requests = 0
        override fun requestRefresh() {
            requests++
        }
    }

    private fun TestScope.startSync(): GeoRestrictionSync =
        GeoRestrictionSync(monitor, scheduler, backgroundScope).also { it.start() }

    private fun TestScope.emitAndSettle(status: NetworkStatus) {
        networkEvents.tryEmit(status)
        advanceTimeBy(SETTLE_MS)
        runCurrent()
    }

    @Test
    fun givenAppStartsOnline_whenSyncStarts_thenOneRefreshRequested() = runTest {
        startSync()
        emitAndSettle(WIFI)
        assertEquals(1, scheduler.requests)
    }

    @Test
    fun givenAppStartsOffline_whenNetworkRestored_thenRefreshRequested() = runTest {
        startSync()
        emitAndSettle(NetworkStatus.Offline)
        assertEquals(0, scheduler.requests)

        emitAndSettle(WIFI)
        assertEquals(1, scheduler.requests)
    }

    @Test
    fun givenOnline_whenGoingOfflineAndBack_thenRefreshRequestedAgain() = runTest {
        startSync()
        emitAndSettle(WIFI)
        emitAndSettle(NetworkStatus.Offline)
        emitAndSettle(WIFI)
        assertEquals(2, scheduler.requests)
    }

    @Test
    fun givenUnchangedConnection_whenStatusRepeats_thenNoExtraRefresh() = runTest {
        startSync()
        emitAndSettle(WIFI)
        emitAndSettle(WIFI)
        emitAndSettle(WIFI)
        assertEquals(1, scheduler.requests)
    }

    @Test
    fun givenNetworkSwitch_whenWifiReplacedByMobile_thenRefreshRequested() = runTest {
        startSync()
        emitAndSettle(WIFI)
        emitAndSettle(MOBILE)
        assertEquals(2, scheduler.requests)
    }

    @Test
    fun givenRapidFlapping_whenEventsArriveWithinSettleTime_thenSingleRefresh() = runTest {
        startSync()
        listOf(WIFI, NetworkStatus.Offline, MOBILE, NetworkStatus.Offline, WIFI, MOBILE).forEach {
            networkEvents.tryEmit(it)
            advanceTimeBy(100)
        }
        advanceTimeBy(SETTLE_MS)
        runCurrent()
        assertEquals(1, scheduler.requests)
    }

    @Test
    fun givenSyncAlreadyStarted_whenStartedAgain_thenEventsHandledOnce() = runTest {
        val sync = startSync()
        sync.start()
        emitAndSettle(WIFI)
        assertEquals(1, scheduler.requests)
    }

    private companion object {
        const val SETTLE_MS = 1_100L
        val WIFI = NetworkStatus.Online(networkHandle = 100)
        val MOBILE = NetworkStatus.Online(networkHandle = 200)
    }
}
