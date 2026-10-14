package com.vplayed.feature.auth.presentation.viewmodel

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.vplayed.R
import com.vplayed.shared.auth.AuthMessage
import com.vplayed.shared.auth.AuthMessageKey

/*
 * Text for the shared layer's sign-in messages ([AuthMessage]): the server's own words when safe to
 * show, else a key this app translates ([stringRes]). The choice between the two is shared
 * (`AuthMessage.serverOr`).
 */

@StringRes
fun AuthMessageKey.stringRes(): Int = when (this) {
    AuthMessageKey.DEVICE_LIMIT -> R.string.login_device_limit
    AuthMessageKey.LOGIN_FAILED -> R.string.login_failed
    AuthMessageKey.GUEST_LOGIN_FAILED -> R.string.login_failed
    AuthMessageKey.OTP_INVALID -> R.string.otp_invalid
    AuthMessageKey.REGISTRATION_REQUIRED -> R.string.otp_registration_pending
    AuthMessageKey.DEVICE_SIGN_OUT_FAILED -> R.string.otp_device_sign_out_failed
    AuthMessageKey.PROFILE_SETUP_PENDING -> R.string.otp_new_user_pending
    AuthMessageKey.MULTI_PROFILE_PENDING -> R.string.otp_multi_profile_pending
    // The app explains timeouts with its connection message, as for every other request.
    AuthMessageKey.ERROR_NETWORK, AuthMessageKey.ERROR_TIMEOUT -> R.string.error_network
    AuthMessageKey.ERROR_UNAUTHORIZED -> R.string.error_unauthorized
    AuthMessageKey.ERROR_SERVER -> R.string.error_server
    AuthMessageKey.ERROR_PARSING -> R.string.error_parsing
    AuthMessageKey.ERROR_UNKNOWN -> R.string.error_unknown
}

/** The text to show for [message] in a composable. */
@Composable
fun AuthMessage.Companion.textOf(message: AuthMessage): String = when (message) {
    is AuthMessage.Server -> message.text
    is AuthMessage.Localized -> stringResource(message.key.stringRes())
}

/** The text to show for this message outside composition (Toasts). */
fun AuthMessage.resolve(context: Context): String = when (this) {
    is AuthMessage.Server -> text
    is AuthMessage.Localized -> context.getString(key.stringRes())
}
