package com.vplayed.core.common.config

import com.vplayed.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Runs once per build variant (`test<Variant>UnitTest`) against that variant's generated
 * BuildConfig, so every variant verifies its own application ID, server and logging settings.
 */
class BuildVariantConfigTest {

    private data class Expected(
        val applicationId: String,
        val environment: AppEnvironment,
        val serverUrl: String?,
        val imageBaseUrl: String?,
        val loggingEnabled: Boolean,
        val debuggable: Boolean,
    )

    private val expectedByBuildType = mapOf(
        "dev" to Expected("com.vplayed.dev", AppEnvironment.DEVELOPMENT, "https://api-devgs.vplayed.com/", DEV_IMAGES, true, true),
        "qa" to Expected("com.vplayed.qa", AppEnvironment.QA, "https://api-qags.vplayed.com/", QA_IMAGES, true, true),
        "stage" to Expected("com.vplayed.stage", AppEnvironment.STAGING, "https://api-stagings.vplayed.com/", QA_IMAGES, false, false),
        "demo" to Expected("com.vplayed", AppEnvironment.DEMO, "https://api-demogs.vplayed.com/", DEMO_IMAGES, false, false),
        // Production URL is not final yet; only its safety properties are asserted.
        "release" to Expected("com.vplayed", AppEnvironment.PRODUCTION, null, null, false, false),
    )

    private val expected = requireNotNull(expectedByBuildType[BuildConfig.BUILD_TYPE]) {
        "No expectations for build type '${BuildConfig.BUILD_TYPE}'. Add it to this test."
    }
    private val config = AppConfig.fromBuildConfig()

    @Test
    fun givenVariant_whenReadingApplicationId_thenSuffixMatchesBuildType() {
        assertEquals(expected.applicationId, BuildConfig.APPLICATION_ID)
    }

    @Test
    fun givenVariant_whenResolvingConfig_thenEnvironmentMatchesBuildType() {
        assertEquals(expected.environment, config.environment)
    }

    @Test
    fun givenVariant_whenResolvingConfig_thenServerUrlMatchesEnvironment() {
        expected.serverUrl?.let { assertEquals(it, config.serverUrl) }
        assertTrue("Server URL must use https", config.serverUrl.startsWith("https://"))
        assertTrue("Server URL must end with '/'", config.serverUrl.endsWith("/"))
    }

    @Test
    fun givenVariant_whenResolvingConfig_thenImageBaseUrlMatchesEnvironment() {
        expected.imageBaseUrl?.let { assertEquals(it, config.imageBaseUrl) }
        assertTrue(config.imageBaseUrl.startsWith("https://") && config.imageBaseUrl.endsWith("/"))
    }

    @Test
    fun givenVariant_whenResolvingConfig_thenLoggingAndDebuggableMatchBuildType() {
        assertEquals(expected.loggingEnabled, config.loggingEnabled)
        assertEquals(expected.debuggable, config.isDebuggable)
    }

    @Test
    fun givenVariant_whenResolvingConfig_thenTransportKeyComesFromBuildConfigAndIsNeverPrinted() {
        assertEquals(BuildConfig.SALT_KEY, config.transportKey)
        if (config.transportKey.isNotEmpty()) {
            assertFalse(config.transportKey in config.toString())
        }
    }

    @Test
    fun givenProductionEnvironment_whenResolvingConfig_thenNoNonProductionServerOrLogging() {
        if (!config.environment.isProduction) return
        val nonProductionHosts = listOf("devgs", "qags", "stagings", "demogs", "gsvp-dev", "gsvp-qa", "gsvp-demo")
        assertFalse(nonProductionHosts.any { it in config.serverUrl || it in config.imageBaseUrl })
        assertFalse(config.loggingEnabled)
        assertFalse(config.isDebuggable)
    }

    private companion object {
        const val DEV_IMAGES = "https://gsvp-dev.s3.ap-south-1.amazonaws.com/"
        const val QA_IMAGES = "https://gsvp-qa.s3.ap-south-1.amazonaws.com/"
        const val DEMO_IMAGES = "https://gsvp-demo.s3.ap-south-1.amazonaws.com/"
    }
}
