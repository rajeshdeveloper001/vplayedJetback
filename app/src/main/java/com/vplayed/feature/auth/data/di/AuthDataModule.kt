package com.vplayed.feature.auth.data.di

import com.vplayed.feature.auth.data.SharedAuthRepository
import com.vplayed.feature.auth.data.remote.AuthApi
import com.vplayed.feature.auth.domain.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import retrofit2.Retrofit
import retrofit2.create

/** Retrofit API of [com.vplayed.feature.auth.data.DefaultAuthRepository], unused since Phase 3; removed in Phase 7. */
@Module
@InstallIn(SingletonComponent::class)
object AuthNetworkModule {
    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create()
}

/**
 * Login goes through the KMP shared layer (Phase 3). Binding [com.vplayed.feature.auth.data.DefaultAuthRepository]
 * here instead restores the Retrofit implementation. Replaced by a fake in instrumented tests (`TestModules.kt`).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AuthRepositoryModule {
    @Binds
    abstract fun bindAuthRepository(repository: SharedAuthRepository): AuthRepository
}
