package com.vplayed.feature.auth.data.di

import com.vplayed.feature.auth.data.DefaultAuthRepository
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

@Module
@InstallIn(SingletonComponent::class)
object AuthNetworkModule {
    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create()
}

/** Replaced by a fake in instrumented tests (`TestModules.kt`). */
@Module
@InstallIn(SingletonComponent::class)
abstract class AuthRepositoryModule {
    @Binds
    abstract fun bindAuthRepository(repository: DefaultAuthRepository): AuthRepository
}
