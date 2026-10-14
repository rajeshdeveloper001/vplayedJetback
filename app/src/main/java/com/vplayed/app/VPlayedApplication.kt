package com.vplayed.app

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.request.crossfade
import com.vplayed.core.geo.GeoRestrictionSync
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class VPlayedApplication : Application(), SingletonImageLoader.Factory {

    @Inject
    lateinit var geoRestrictionSync: GeoRestrictionSync

    override fun onCreate() {
        super.onCreate()
        // App-wide IP / geo-token refresh on connectivity changes. Runs in the background; no
        // screen waits for it.
        geoRestrictionSync.start()
    }

    /** App-wide image loader (Coil): memory + disk cache, network via OkHttp, crossfade on load. */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .crossfade(true)
            .build()
}
