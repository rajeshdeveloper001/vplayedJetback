package com.vplayed.core.shared

import com.vplayed.core.common.config.AppConfig
import com.vplayed.shared.PlatformDependencies
import com.vplayed.shared.SharedGraph
import com.vplayed.shared.core.crypto.platformCryptoPrimitives
import com.vplayed.shared.core.network.createOkHttpEngine
import com.vplayed.shared.core.platform.platformLogger
import com.vplayed.shared.geo.GeoStore
import com.vplayed.shared.session.SessionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.OkHttpClient

/**
 * The one [SharedGraph] (Kotlin Multiplatform shared layer, ../kmp) of the process. Features move
 * onto it phase by phase (KMP_PLANNING/KMP_MIGRATION_PLAN.md). Login runs on it since Phase 3
 * (shared `AuthRepository` and controllers); Home and geo restriction since Phase 4; the session
 * since Phase 5 (shared `SessionManager`, provided by `SessionManagerModule`).
 */
@Module
@InstallIn(SingletonComponent::class)
object SharedModule {

    @Provides
    @Singleton
    fun provideSharedGraph(
        config: AppConfig,
        deviceInfo: AndroidSharedDeviceInfo,
        sessionManager: SessionManager,
        geoStore: GeoStore,
        okHttpClient: OkHttpClient,
    ): SharedGraph {
        // The app's client (no interceptors): the shared layer adds the VPlayed headers and its own
        // redacting log itself.
        return SharedGraph(
            config = config.toSharedConfig(),
            platform = PlatformDependencies(
                deviceInfo = deviceInfo,
                logger = platformLogger(),
                engineFactory = { createOkHttpEngine(okHttpClient) },
                cryptoPrimitives = platformCryptoPrimitives(),
            ),
            sessionManager = sessionManager,
            geoStore = geoStore,
        )
    }
}
