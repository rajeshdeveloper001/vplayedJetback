package com.vplayed.feature.auth.domain

import com.vplayed.shared.auth.DeviceLimit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Passes the device list from Login (HTTP 202 on email-login) to the OTP screen without putting
 * other devices' credentials into navigation arguments. In memory only; [take] clears it.
 */
@Singleton
class DeviceLimitHandoff @Inject constructor() {
    @Volatile
    private var pending: DeviceLimit? = null

    fun put(limit: DeviceLimit?) {
        pending = limit
    }

    fun take(): DeviceLimit? = pending.also { pending = null }
}
