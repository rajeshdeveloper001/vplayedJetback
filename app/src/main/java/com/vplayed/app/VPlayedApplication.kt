package com.vplayed.app

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.request.crossfade
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.vplayed.core.common.ActivityTracker
import com.vplayed.core.common.config.AppConfig
import com.vplayed.core.geo.GeoRestrictionSync
import com.vplayed.core.startup.AppLaunchTasks
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class VPlayedApplication : Application(), SingletonImageLoader.Factory {

    @Inject
    lateinit var geoRestrictionSync: GeoRestrictionSync

    @Inject
    lateinit var appLaunchTasks: AppLaunchTasks

    @Inject
    lateinit var activityTracker: ActivityTracker

    @Inject
    lateinit var appConfig: AppConfig

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(activityTracker)
        startFirebase()
        // Common settings (login method, guest access) and the automatic guest session.
        appLaunchTasks.start()
        // App-wide IP / geo-token refresh on connectivity changes. Runs in the background; no
        // screen waits for it.
        geoRestrictionSync.start()
    }

    /**
     * Firebase sends phone sign-in codes. It starts from this build's settings (one Firebase Android
     * app per application ID) instead of google-services.json; without them phone sign-in is off.
     */
    private fun startFirebase() {
        val firebase = appConfig.firebase ?: return
        if (FirebaseApp.getApps(this).isNotEmpty()) return
        FirebaseApp.initializeApp(
            this,
            FirebaseOptions.Builder()
                .setApiKey(firebase.apiKey)
                .setApplicationId(firebase.applicationId)
                .setProjectId(firebase.projectId)
                .setGcmSenderId(firebase.senderId)
                .build(),
        )
    }

    /** App-wide image loader (Coil): memory + disk cache, network via OkHttp, crossfade on load. */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .crossfade(true)
            .build()
}
