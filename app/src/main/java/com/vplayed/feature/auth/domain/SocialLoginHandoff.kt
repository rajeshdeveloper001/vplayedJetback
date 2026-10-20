package com.vplayed.feature.auth.domain

import com.vplayed.shared.auth.DeviceLimit
import com.vplayed.shared.auth.SocialCredential
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Passes a Google sign-in that hit the device limit (`auth/login` 202) from Login to the OTP
 * screen's device step, which signs in again with the same credential. Tokens never go into
 * navigation arguments. In memory only; [take] clears it.
 */
@Singleton
class SocialLoginHandoff @Inject constructor() {
    @Volatile
    private var pending: Pair<SocialCredential, DeviceLimit>? = null

    fun put(credential: SocialCredential, limit: DeviceLimit) {
        pending = credential to limit
    }

    fun take(): Pair<SocialCredential, DeviceLimit>? = pending.also { pending = null }
}
