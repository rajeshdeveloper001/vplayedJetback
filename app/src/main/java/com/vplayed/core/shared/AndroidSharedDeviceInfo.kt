package com.vplayed.core.shared

import com.vplayed.core.network.DeviceIdProvider
import com.vplayed.core.network.DeviceNameProvider
import com.vplayed.shared.core.platform.DeviceInfo
import java.util.Locale
import javax.inject.Inject

/**
 * Device facts for the shared layer, from the same providers `ApiHeadersInterceptor` uses, so
 * shared requests send identical `device_id` and `X-Language-Code` values; `deviceDetail` on login
 * is the [DeviceNameProvider] name, as in the previous app.
 */
class AndroidSharedDeviceInfo @Inject constructor(
    private val deviceIdProvider: DeviceIdProvider,
    private val deviceNameProvider: DeviceNameProvider,
) : DeviceInfo {
    override val deviceId: String? get() = deviceIdProvider.deviceId

    override val deviceName: String get() = deviceNameProvider.deviceName

    // Read per request, like ApiHeadersInterceptor, so a locale change applies immediately.
    override fun languageCode(): String = Locale.getDefault().language
}
