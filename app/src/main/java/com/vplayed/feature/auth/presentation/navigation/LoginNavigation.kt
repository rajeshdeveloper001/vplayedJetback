package com.vplayed.feature.auth.presentation.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.fragment.compose.AndroidFragment
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.vplayed.feature.auth.presentation.LoginFragment
import com.vplayed.shared.auth.OtpChannelKind
import kotlinx.serialization.Serializable

@Serializable
data object LoginDestination

/**
 * Login destination. The screen is a Fragment ([LoginFragment]) embedded with `AndroidFragment`,
 * so Navigation Compose stays the app's only navigation framework.
 */
fun NavGraphBuilder.loginScreen(
    onClose: () -> Unit,
    onOpenOtp: (destination: String, startWithDeviceLimit: Boolean, channel: OtpChannelKind, dialCode: String?) -> Unit,
) {
    composable<LoginDestination> {
        val currentOnClose by rememberUpdatedState(onClose)
        val currentOnOpenOtp by rememberUpdatedState(onOpenOtp)
        AndroidFragment<LoginFragment>(
            modifier = Modifier.fillMaxSize(),
            onUpdate = { fragment ->
                fragment.onCloseRequested = { currentOnClose() }
                fragment.onOpenOtp = { destination, devices, channel, dialCode -> currentOnOpenOtp(destination, devices, channel, dialCode) }
            },
        )
    }
}
