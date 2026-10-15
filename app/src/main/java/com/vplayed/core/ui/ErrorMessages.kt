package com.vplayed.core.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.vplayed.R
import com.vplayed.shared.core.error.AppError as SharedAppError

/** User-facing message for a shared-layer error (Home, sign-out and other shared features). */
@Composable
fun SharedAppError.userMessage(): String = stringResource(userMessageRes())

/** String resource of [userMessage], for one-time messages (e.g. Toasts) built outside composition. A timeout reads as a network problem. */
@StringRes
fun SharedAppError.userMessageRes(): Int = when (this) {
    SharedAppError.Network, SharedAppError.Timeout -> R.string.error_network
    SharedAppError.Unauthorized -> R.string.error_unauthorized
    is SharedAppError.Server -> R.string.error_server
    SharedAppError.Parsing -> R.string.error_parsing
    is SharedAppError.Configuration, is SharedAppError.Unknown -> R.string.error_unknown
}
