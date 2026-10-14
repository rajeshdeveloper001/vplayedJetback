package com.vplayed.core.network

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Identifier sent as the `device_id` header (the backend uses it for device limits). */
interface DeviceIdProvider {
    /** Null when the platform does not provide one; the header is then omitted. */
    val deviceId: String?
}

// HardwareIds: ANDROID_ID is required by the backend's device-limit contract (same value the
// previous app sent); it is app-signing-key scoped since Android 8 and not used for ads.
@SuppressLint("HardwareIds")
@Singleton
class AndroidDeviceIdProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) : DeviceIdProvider {
    override val deviceId: String? by lazy {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)?.takeIf { it.isNotBlank() }
    }
}
