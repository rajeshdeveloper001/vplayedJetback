package com.vplayed.core.common.config

/** Backend environment a build talks to. Resolved from the build type, never chosen at runtime. */
enum class AppEnvironment(val id: String) {
    DEVELOPMENT("dev"),
    QA("qa"),
    STAGING("stage"),
    DEMO("demo"),
    PRODUCTION("production"),
    ;

    val isProduction: Boolean get() = this == PRODUCTION

    companion object {
        fun fromId(id: String): AppEnvironment =
            entries.firstOrNull { it.id == id }
                ?: throw IllegalArgumentException("Unknown environment id '$id'")
    }
}

/**
 * Typed, immutable view of the build-time environment configuration.
 * The only class allowed to read environment values; everything else injects [AppConfig].
 */
data class AppConfig(
    val environment: AppEnvironment,
    val serverUrl: String,
    /** Base for relative image paths returned by the API (ends with '/'). */
    val imageBaseUrl: String,
    val loggingEnabled: Boolean,
    val isDebuggable: Boolean,
    val versionName: String,
    /** Passphrase for request-payload encryption (`SALT_KEY`). Empty when not configured. Secret. */
    val transportKey: String = "",
) {
    // Keeps the secret out of logs and crash reports.
    override fun toString(): String =
        "AppConfig(environment=$environment, serverUrl=$serverUrl, imageBaseUrl=$imageBaseUrl, " +
            "loggingEnabled=$loggingEnabled, isDebuggable=$isDebuggable, versionName=$versionName, " +
            "transportKey=${if (transportKey.isEmpty()) "unset" else "***"})"

    companion object
}
