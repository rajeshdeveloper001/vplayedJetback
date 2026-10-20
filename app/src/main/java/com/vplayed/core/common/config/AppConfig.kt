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

/** The Firebase app that sends phone sign-in codes (started without google-services.json). */
data class FirebaseConfig(
    val apiKey: String,
    val applicationId: String,
    val projectId: String,
    val senderId: String,
) {
    override fun toString(): String = "FirebaseConfig(projectId=$projectId, ***)"
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
    /** Null when the build has no Firebase settings: phone sign-in falls back to email. */
    val firebase: FirebaseConfig? = null,
    /** Google's web OAuth client ID for Credential Manager; empty hides Google sign-in. */
    val googleWebClientId: String = "",
) {
    // Keeps the secret out of logs and crash reports.
    override fun toString(): String =
        "AppConfig(environment=$environment, serverUrl=$serverUrl, imageBaseUrl=$imageBaseUrl, " +
            "loggingEnabled=$loggingEnabled, isDebuggable=$isDebuggable, versionName=$versionName, " +
            "transportKey=${if (transportKey.isEmpty()) "unset" else "***"}, " +
            "firebase=${if (firebase == null) "unset" else "set"}, googleWebClientId=${if (googleWebClientId.isEmpty()) "unset" else "set"})"

    companion object
}
