package com.vplayed.feature.splashscreen.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Splash timing. Injected so instrumented tests can shorten it (see `TestSplashModule`). */
data class SplashConfig(val minDisplayMillis: Long)

@Module
@InstallIn(SingletonComponent::class)
object SplashModule {
    /** The splash stays at least this long, even when startup work finishes sooner. */
    const val MIN_DISPLAY_MILLIS = 3_000L

    @Provides
    fun provideSplashConfig(): SplashConfig = SplashConfig(minDisplayMillis = MIN_DISPLAY_MILLIS)
}
