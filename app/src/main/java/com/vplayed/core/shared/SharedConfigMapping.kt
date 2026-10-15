package com.vplayed.core.shared

import com.vplayed.core.common.config.AppConfig
import com.vplayed.shared.core.config.ClientPlatform
import com.vplayed.shared.core.config.ServerEnvironment
import com.vplayed.shared.core.config.SharedConfig

/**
 * The shared layer's configuration, built from [AppConfig] (still the only reader of BuildConfig),
 * so both layers always talk to the same environment with the same transport key.
 */
fun AppConfig.toSharedConfig(): SharedConfig = SharedConfig(
    // Both enums use the build-type ids (dev, qa, stage, demo, production).
    environment = ServerEnvironment.fromId(environment.id),
    serverUrl = serverUrl,
    imageBaseUrl = imageBaseUrl,
    loggingEnabled = loggingEnabled,
    appVersion = versionName,
    platform = ClientPlatform.ANDROID,
    transportKey = transportKey,
)
