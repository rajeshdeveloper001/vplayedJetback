package com.vplayed.core.geo

import com.vplayed.core.geo.work.GeoRefreshWorker
import com.vplayed.core.geo.work.GeoWorkDecision
import com.vplayed.core.geo.work.toWorkDecision
import org.junit.Assert.assertEquals
import org.junit.Test

class GeoWorkDecisionTest {

    @Test
    fun givenSuccessOrInvalidResponse_whenDeciding_thenWorkSucceedsWithoutRetry() {
        assertEquals(GeoWorkDecision.SUCCESS, GeoRefreshOutcome.Success.toWorkDecision(0))
        assertEquals(GeoWorkDecision.SUCCESS, GeoRefreshOutcome.InvalidResponse.toWorkDecision(0))
    }

    @Test
    fun givenTransientFailure_whenAttemptsRemain_thenRetries() {
        assertEquals(GeoWorkDecision.RETRY, GeoRefreshOutcome.TransientFailure.toWorkDecision(0))
        assertEquals(GeoWorkDecision.RETRY, GeoRefreshOutcome.TransientFailure.toWorkDecision(GeoRefreshWorker.MAX_ATTEMPTS - 2))
    }

    @Test
    fun givenTransientFailure_whenLastAttempt_thenStopsRetrying() {
        assertEquals(GeoWorkDecision.FAILURE, GeoRefreshOutcome.TransientFailure.toWorkDecision(GeoRefreshWorker.MAX_ATTEMPTS - 1))
    }
}
