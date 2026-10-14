package com.vplayed.core.common.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppEnvironmentTest {

    @Test
    fun givenKnownIds_whenResolving_thenReturnsMatchingEnvironment() {
        AppEnvironment.entries.forEach { env -> assertEquals(env, AppEnvironment.fromId(env.id)) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun givenUnknownId_whenResolving_thenThrows() {
        AppEnvironment.fromId("prod-typo")
    }

    @Test
    fun givenEnvironments_whenCheckingProduction_thenOnlyProductionIsProduction() {
        assertTrue(AppEnvironment.PRODUCTION.isProduction)
        AppEnvironment.entries.filter { it != AppEnvironment.PRODUCTION }.forEach { assertFalse(it.isProduction) }
    }
}
