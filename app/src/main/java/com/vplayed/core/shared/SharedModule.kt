package com.vplayed.core.shared

import com.vplayed.core.common.config.AppConfig
import com.vplayed.shared.PlatformDependencies
import com.vplayed.shared.SharedGraph
import com.vplayed.shared.core.crypto.platformCryptoPrimitives
import com.vplayed.shared.core.network.createOkHttpEngine
import com.vplayed.shared.core.platform.platformLogger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.OkHttpClient

/**
 * The one [SharedGraph] (Kotlin Multiplatform shared layer, ../kmp) of the process. Features move
 * onto it phase by phase (KMP_PLANNING/KMP_MIGRATION_PLAN.md). Login uses it since Phase 3
 * (`SharedAuthRepository`); Home and geo still use Retrofit.
 */
@Module
@InstallIn(SingletonComponent::class)
object SharedModule {

    @Provides
    @Singleton
    fun provideSharedGraph(
        config: AppConfig,
        deviceInfo: AndroidSharedDeviceInfo,
        credentials: AndroidRequestCredentials,
        okHttpClient: OkHttpClient,
    ): SharedGraph {
        // Shares the app client's connection pool and dispatcher, but none of its interceptors:
        // the shared layer adds the VPlayed headers and its own redacting log itself.
        val engineClient = okHttpClient.newBuilder()
            .apply {
                interceptors().clear()
                networkInterceptors().clear()
            }
            .build()
        return SharedGraph(
            config = config.toSharedConfig(),
            platform = PlatformDependencies(
                deviceInfo = deviceInfo,
                logger = platformLogger(),
                engineFactory = { createOkHttpEngine(engineClient) },
                cryptoPrimitives = platformCryptoPrimitives(),
                requestCredentials = credentials,
            ),
        )
    }
}
