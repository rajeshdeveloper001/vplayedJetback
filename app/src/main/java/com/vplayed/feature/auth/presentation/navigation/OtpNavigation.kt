package com.vplayed.feature.auth.presentation.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.core.os.bundleOf
import androidx.fragment.compose.AndroidFragment
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.vplayed.feature.auth.presentation.OtpFragment
import com.vplayed.feature.auth.presentation.viewmodel.OtpViewModel
import kotlinx.serialization.Serializable

/**
 * Verification of an email login. [startWithDeviceLimit] opens the device-limit step directly (the
 * email-login request already answered "device limit"); the device list itself travels through
 * `DeviceLimitHandoff`, never through navigation arguments.
 */
@Serializable
data class OtpDestination(val email: String, val startWithDeviceLimit: Boolean = false)

/** OTP destination: [OtpFragment] embedded with `AndroidFragment`, like Login. */
fun NavGraphBuilder.otpScreen(onBack: () -> Unit, onClose: () -> Unit, onAuthenticated: () -> Unit) {
    composable<OtpDestination> { entry ->
        val route = entry.toRoute<OtpDestination>()
        val arguments = remember(route) {
            bundleOf(
                OtpViewModel.ARG_EMAIL to route.email,
                OtpViewModel.ARG_START_WITH_DEVICE_LIMIT to route.startWithDeviceLimit,
            )
        }
        val currentOnBack by rememberUpdatedState(onBack)
        val currentOnClose by rememberUpdatedState(onClose)
        val currentOnAuthenticated by rememberUpdatedState(onAuthenticated)
        AndroidFragment<OtpFragment>(
            modifier = Modifier.fillMaxSize(),
            arguments = arguments,
            onUpdate = { fragment ->
                fragment.onBackRequested = { currentOnBack() }
                fragment.onCloseRequested = { currentOnClose() }
                fragment.onAuthenticated = { currentOnAuthenticated() }
            },
        )
    }
}
