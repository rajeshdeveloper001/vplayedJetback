package com.vplayed.core.network

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Name of this device as the backend shows it in a user's device list (`deviceDetail`). */
interface DeviceNameProvider {
    val deviceName: String
}

// InlinedApi: DEVICE_NAME is API 25; on API 24 the lookup finds nothing and the fallback is used.
@SuppressLint("InlinedApi")
@Singleton
class AndroidDeviceNameProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) : DeviceNameProvider {
    // The user-chosen name when set (Settings › About phone), else "<brand> <device>", as the previous app did.
    override val deviceName: String by lazy {
        Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME)
            ?.takeIf { it.isNotBlank() }
            ?: "${Build.BRAND} ${Build.DEVICE}"
    }
}
