package com.vplayed.core.common

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The activity in the foreground, for platform SDKs that need one outside a screen (Firebase phone
 * sign-in may show a reCAPTCHA). Held weakly; [VPlayedApplication][com.vplayed.app.VPlayedApplication]
 * registers it.
 */
@Singleton
class ActivityTracker @Inject constructor() : Application.ActivityLifecycleCallbacks {
    @Volatile
    private var resumed: WeakReference<Activity>? = null

    /** The resumed activity, or null when the app is in the background. */
    val current: Activity? get() = resumed?.get()

    override fun onActivityResumed(activity: Activity) {
        resumed = WeakReference(activity)
    }

    override fun onActivityPaused(activity: Activity) {
        if (resumed?.get() === activity) resumed = null
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

    override fun onActivityStarted(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    override fun onActivityDestroyed(activity: Activity) = Unit
}
