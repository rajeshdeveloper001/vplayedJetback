package com.vplayed.feature.auth.data.di

import com.vplayed.shared.SharedGraph
import com.vplayed.shared.auth.AuthRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
/**
 * Sign-in requests go through the KMP shared layer (`SharedGraph.authRepository`), used by the
 * shared controllers behind `LoginViewModel` and `OtpViewModel`. Replaced by a fake in
 * instrumented tests (`TestModules.kt`).
 */
@Module
@InstallIn(SingletonComponent::class)
object AuthRepositoryModule {
    @Provides
    fun provideAuthRepository(graph: SharedGraph): AuthRepository = graph.authRepository
}
