package com.vplayed.core.geo.di

import com.vplayed.core.geo.work.GeoRefreshScheduler
import com.vplayed.core.geo.work.WorkManagerGeoRefreshScheduler
import com.vplayed.shared.SharedGraph
import com.vplayed.shared.geo.GeoRepository
import com.vplayed.shared.geo.GeoStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Geo restriction runs on the shared layer since Phase 4 (../kmp `geo/`): [GeoStore] holds the IP,
 * geo token and currency, [GeoRepository] refreshes it. This app decides when to refresh
 * ([com.vplayed.core.geo.GeoRestrictionSync], WorkManager).
 */
@Module
@InstallIn(SingletonComponent::class)
object GeoModule {

    /** Created before the [SharedGraph], because the request headers of both network stacks read it. */
    @Provides
    @Singleton
    fun provideGeoStore(): GeoStore = GeoStore()

    @Provides
    fun provideGeoRepository(graph: SharedGraph): GeoRepository = graph.geoRepository
}

@Module
@InstallIn(SingletonComponent::class)
abstract class GeoBindingsModule {
    @Binds
    abstract fun bindGeoRefreshScheduler(scheduler: WorkManagerGeoRefreshScheduler): GeoRefreshScheduler
}
