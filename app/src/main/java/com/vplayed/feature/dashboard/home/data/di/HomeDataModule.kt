package com.vplayed.feature.dashboard.home.data.di

import com.vplayed.shared.SharedGraph
import com.vplayed.shared.home.HomeRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Home data comes from the shared layer since Phase 4 (../kmp `home/`: endpoints, mapping, paging
 * rules). A module of its own so UI tests can replace the repository.
 */
@Module
@InstallIn(SingletonComponent::class)
object HomeRepositoryModule {
    @Provides
    fun provideHomeRepository(graph: SharedGraph): HomeRepository = graph.homeRepository
}
