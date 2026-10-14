package com.vplayed.core.shared

import com.vplayed.core.common.config.AppConfig
import com.vplayed.core.common.config.fromBuildConfig
import com.vplayed.shared.core.config.ClientPlatform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/** Runs once per build type (testDevUnitTest … testReleaseUnitTest), so every environment is mapped. */
class SharedConfigMappingTest {

    private val appConfig = AppConfig.fromBuildConfig()
    private val shared = appConfig.toSharedConfig()

    @Test
    fun givenVariantConfig_whenMapped_thenSharedLayerUsesTheSameEnvironment() {
        assertEquals(appConfig.environment.id, shared.environment.id)
        assertEquals(appConfig.serverUrl, shared.serverUrl)
        assertEquals(appConfig.imageBaseUrl, shared.imageBaseUrl)
        assertEquals(appConfig.loggingEnabled, shared.loggingEnabled)
        assertEquals(appConfig.versionName, shared.appVersion)
        assertEquals(ClientPlatform.ANDROID, shared.platform)
    }

    @Test
    fun givenVariantConfig_whenMapped_thenTransportKeyIsPassedButNeverPrinted() {
        assertEquals(appConfig.transportKey, shared.transportKey)
        if (appConfig.transportKey.isNotEmpty()) {
            assertFalse(shared.toString().contains(appConfig.transportKey))
        }
    }
}
