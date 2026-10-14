package com.vplayed.feature.dashboard.home.data.di

import com.vplayed.core.common.config.AppConfig
import com.vplayed.feature.dashboard.home.data.DefaultHomeRepository
import com.vplayed.feature.dashboard.home.data.mapper.HomeMapper
import com.vplayed.feature.dashboard.home.data.remote.HomeApi
import com.vplayed.feature.dashboard.home.domain.HomeRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.create

@Module
@InstallIn(SingletonComponent::class)
object HomeNetworkModule {
    @Provides
    fun provideHomeApi(retrofit: Retrofit): HomeApi = retrofit.create()

    @Provides
    fun provideHomeMapper(config: AppConfig): HomeMapper = HomeMapper(config.imageBaseUrl)
}

/** Separate from [HomeNetworkModule] so UI tests can replace only the repository. */
@Module
@InstallIn(SingletonComponent::class)
abstract class HomeRepositoryModule {
    @Binds
    abstract fun bindHomeRepository(impl: DefaultHomeRepository): HomeRepository
}
