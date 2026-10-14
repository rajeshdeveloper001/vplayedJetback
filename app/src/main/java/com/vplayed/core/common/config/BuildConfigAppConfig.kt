package com.vplayed.core.common.config

import com.vplayed.BuildConfig

/** Builds [AppConfig] from the generated BuildConfig of the current variant. */
fun AppConfig.Companion.fromBuildConfig(): AppConfig = AppConfig(
    environment = AppEnvironment.fromId(BuildConfig.ENVIRONMENT),
    serverUrl = BuildConfig.SERVER_URL,
    imageBaseUrl = BuildConfig.IMAGE_BASE_URL,
    loggingEnabled = BuildConfig.LOGGING_ENABLED,
    isDebuggable = BuildConfig.DEBUG,
    versionName = BuildConfig.VERSION_NAME,
    transportKey = BuildConfig.SALT_KEY,
)
