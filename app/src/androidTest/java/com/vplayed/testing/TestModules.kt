package com.vplayed.testing

import com.vplayed.core.session.SessionRepository
import com.vplayed.core.session.di.SessionRepositoryModule
import com.vplayed.feature.auth.data.di.AuthRepositoryModule
import com.vplayed.feature.auth.domain.AuthRepository
import com.vplayed.feature.dashboard.home.data.di.HomeRepositoryModule
import com.vplayed.feature.dashboard.home.domain.HomeRepository
import com.vplayed.feature.splashscreen.di.SplashConfig
import com.vplayed.feature.splashscreen.di.SplashModule
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn

/** Instrumented tests never call the real API: Home data comes from [FakeHomeRepository]. */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [HomeRepositoryModule::class])
abstract class TestHomeRepositoryModule {
    @Binds
    abstract fun bindHomeRepository(fake: FakeHomeRepository): HomeRepository
}

/** Login requests never reach the backend: [FakeAuthRepository] answers them. */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [AuthRepositoryModule::class])
abstract class TestAuthRepositoryModule {
    @Binds
    abstract fun bindAuthRepository(fake: FakeAuthRepository): AuthRepository
}

/** The session lives in memory in instrumented tests: no Keystore, no stored files. */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [SessionRepositoryModule::class])
abstract class TestSessionRepositoryModule {
    @Binds
    abstract fun bindSessionRepository(fake: FakeSessionRepository): SessionRepository
}

/** No 3-second wait in tests; the splash still shows and hands over to Home. */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [SplashModule::class])
object TestSplashModule {
    @Provides
    fun provideSplashConfig(): SplashConfig = SplashConfig(minDisplayMillis = 0)
}
