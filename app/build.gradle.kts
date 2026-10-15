import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

/**
 * Display name shown under the launcher icon. Non-production builds append their environment
 * label so testers can tell installed builds apart.
 */
val appDisplayName = "Vplayed3.0"

/**
 * Build-time description of one backend environment. Every build type maps to exactly one
 * environment; values reach the app through BuildConfig and are read only by
 * `core.common.config.AppConfig`.
 *
 * Any server URL can be overridden without editing this file by setting the Gradle property
 * `vplayed.serverUrl.<id>` (in ~/.gradle/gradle.properties, or with -P on CI).
 */
data class BackendEnvironment(
    val id: String,
    val label: String,
    val defaultServerUrl: String,
    /** Storage bucket that serves the relative image paths returned by the API. */
    val defaultImageBaseUrl: String,
    val loggingEnabled: Boolean,
)

val development = BackendEnvironment(
    "dev", "Dev", "https://api-devgs.vplayed.com/", "https://gsvp-dev.s3.ap-south-1.amazonaws.com/", loggingEnabled = true,
)
val qa = BackendEnvironment(
    "qa", "QA", "https://api-qags.vplayed.com/", "https://gsvp-qa.s3.ap-south-1.amazonaws.com/", loggingEnabled = true,
)
// Stage images are served from the QA bucket (same as the previous app).
val staging = BackendEnvironment(
    "stage", "Stage", "https://api-stagings.vplayed.com/", "https://gsvp-qa.s3.ap-south-1.amazonaws.com/", loggingEnabled = false,
)
val demo = BackendEnvironment(
    "demo", "Demo", "https://api-demogs.vplayed.com/", "https://gsvp-demo.s3.ap-south-1.amazonaws.com/", loggingEnabled = false,
)
// TODO: production server and image URLs not yet provided; placeholders until the release backend is known.
val production = BackendEnvironment(
    "production", "", "https://api.example.com/", "https://images.example.com/", loggingEnabled = false,
)

fun BackendEnvironment.serverUrl(): String =
    providers.gradleProperty("vplayed.serverUrl.$id").getOrElse(defaultServerUrl)

fun BackendEnvironment.imageBaseUrl(): String =
    providers.gradleProperty("vplayed.imageBaseUrl.$id").getOrElse(defaultImageBaseUrl)

// Guard: production must never point at a non-production backend, and every URL must be HTTPS
// with a trailing slash (Retrofit requirement; image paths are appended to the base).
listOf(development, qa, staging, demo, production).forEach { env ->
    listOf(env.serverUrl(), env.imageBaseUrl()).forEach { url ->
        require(url.startsWith("https://") && url.endsWith("/")) {
            "URLs for '${env.id}' must use https and end with '/': $url"
        }
    }
}
listOf(development, qa, staging, demo).forEach { env ->
    require(env.serverUrl() != production.serverUrl()) {
        "Production server URL must differ from the '${env.id}' URL."
    }
}

/**
 * Release signing is read from `keystore.properties` at the project root (git-ignored) or, on CI,
 * from environment variables. When neither is present, release-like builds are produced unsigned.
 */
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use(::load)
}

fun signingValue(propertyKey: String, envKey: String): String? =
    keystoreProperties.getProperty(propertyKey) ?: providers.environmentVariable(envKey).orNull

val releaseStoreFile = signingValue("storeFile", "VPLAYED_KEYSTORE_FILE")

/**
 * Passphrase for the backend's request-payload encryption (`SALT_KEY`, same value as the previous
 * app). Never committed: read from `secrets.properties` at the project root (git-ignored), the
 * Gradle property `vplayed.saltKey`, or the `VPLAYED_SALT_KEY` environment variable on CI.
 * Builds without it still work, but email login reports that it is unavailable.
 */
val secretProperties = Properties().apply {
    val file = rootProject.file("secrets.properties")
    if (file.exists()) file.inputStream().use(::load)
}
val transportKey: String = secretProperties.getProperty("vplayed.saltKey")
    ?: providers.gradleProperty("vplayed.saltKey").orNull
    ?: providers.environmentVariable("VPLAYED_SALT_KEY").orNull
    ?: ""
if (transportKey.isEmpty()) {
    logger.warn("vplayed.saltKey is not set (secrets.properties / -Pvplayed.saltKey / VPLAYED_SALT_KEY); email login is unavailable in this build.")
}

android {
    namespace = "com.vplayed"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.contus.vplayed"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        // Runs instrumented tests on HiltTestApplication so user-flow tests can swap in fake data.
        testInstrumentationRunner = "com.vplayed.testing.HiltTestRunner"
        // Orchestrator runs each test in its own instrumentation and clears app data (DataStore) between tests.
        testInstrumentationRunnerArguments["clearPackageData"] = "true"

        val escapedTransportKey = transportKey.replace("\\", "\\\\").replace("\"", "\\\"")
        buildConfigField("String", "SALT_KEY", "\"$escapedTransportKey\"")
    }

    signingConfigs {
        if (releaseStoreFile != null) {
            create("release") {
                storeFile = rootProject.file(releaseStoreFile)
                storePassword = signingValue("storePassword", "VPLAYED_KEYSTORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "VPLAYED_KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "VPLAYED_KEY_PASSWORD")
            }
        }
    }

    fun com.android.build.api.dsl.ApplicationBuildType.environment(env: BackendEnvironment) {
        buildConfigField("String", "ENVIRONMENT", "\"${env.id}\"")
        buildConfigField("String", "SERVER_URL", "\"${env.serverUrl()}\"")
        buildConfigField("String", "IMAGE_BASE_URL", "\"${env.imageBaseUrl()}\"")
        buildConfigField("boolean", "LOGGING_ENABLED", env.loggingEnabled.toString())
        val name = if (env.label.isEmpty()) appDisplayName else "$appDisplayName ${env.label}"
        resValue("string", "app_name", name)
    }

    fun com.android.build.api.dsl.ApplicationBuildType.releaseLike() {
        isDebuggable = false
        optimization {
            enable = true
            packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
        }
        signingConfig = signingConfigs.findByName("release")
    }

    buildTypes {
        // `debug` is created by AGP and cannot be removed; its variant is disabled in
        // androidComponents below. `dev` and `qa` are the debuggable builds.
        release {
            releaseLike()
            environment(production)
        }
        create("dev") {
            isDebuggable = true
            applicationIdSuffix = ".dev"
            isDefault = true
            enableUnitTestCoverage = true
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += "debug"
            environment(development)
        }
        create("qa") {
            isDebuggable = true
            applicationIdSuffix = ".qa"
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += "debug"
            environment(qa)
        }
        create("stage") {
            releaseLike()
            applicationIdSuffix = ".stage"
            matchingFallbacks += "release"
            environment(staging)
        }
        create("demo") {
            // Demo intentionally shares the base application ID (no suffix).
            releaseLike()
            matchingFallbacks += "release"
            environment(demo)
        }
    }

    // Instrumented tests run against the dev build (the default `debug` is disabled).
    testBuildType = "dev"

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }

    testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
        animationsDisabled = true
    }

    lint {
        abortOnError = true
        checkDependencies = false
    }
}

androidComponents {
    beforeVariants(selector().withBuildType("debug")) { it.enable = false }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.fragment.compose)
    implementation(libs.androidx.work.runtime.ktx)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.navigation.suite)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.okhttp)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // Shared business logic: prefers direct local AAR in app/libs/ if present,
    // otherwise resolves from repository (libs.vplayed.shared)
    val localAar = file("libs/shared-release.aar")
    if (localAar.exists()) {
        implementation(files(localAar))
        // Direct AARs do not bring transitive dependencies automatically:
        implementation(libs.ktor.client.core)
        implementation(libs.ktor.client.okhttp)
    } else {
        implementation(libs.vplayed.shared)
    }

    // Preview rendering for every debuggable build type.
    implementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.hilt.android.testing)
    androidTestUtil(libs.androidx.test.orchestrator)
    // Hosts ComponentActivity for createComposeRule(); only the instrumented test build type (dev) needs it.
    "devImplementation"(libs.androidx.compose.ui.test.manifest)
}
