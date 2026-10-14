package com.vplayed.core.geo

import com.vplayed.core.geo.work.GeoRefreshWorker
import com.vplayed.core.geo.work.GeoWorkDecision
import com.vplayed.core.geo.work.toWorkDecision
import com.vplayed.shared.geo.GeoRefreshOutcome
import org.junit.Assert.assertEquals
import org.junit.Test

class GeoWorkDecisionTest {

    @Test
    fun givenSuccessOrInvalidResponse_whenDeciding_thenWorkSucceedsWithoutRetry() {
        assertEquals(GeoWorkDecision.SUCCESS, GeoRefreshOutcome.SUCCESS.toWorkDecision(0))
        assertEquals(GeoWorkDecision.SUCCESS, GeoRefreshOutcome.INVALID_RESPONSE.toWorkDecision(0))
    }

    @Test
    fun givenTransientFailure_whenAttemptsRemain_thenRetries() {
        assertEquals(GeoWorkDecision.RETRY, GeoRefreshOutcome.TRANSIENT_FAILURE.toWorkDecision(0))
        assertEquals(GeoWorkDecision.RETRY, GeoRefreshOutcome.TRANSIENT_FAILURE.toWorkDecision(GeoRefreshWorker.MAX_ATTEMPTS - 2))
    }

    @Test
    fun givenTransientFailure_whenLastAttempt_thenStopsRetrying() {
        assertEquals(GeoWorkDecision.FAILURE, GeoRefreshOutcome.TRANSIENT_FAILURE.toWorkDecision(GeoRefreshWorker.MAX_ATTEMPTS - 1))
    }
}
