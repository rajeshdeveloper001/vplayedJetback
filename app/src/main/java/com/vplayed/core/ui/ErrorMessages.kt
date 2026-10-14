package com.vplayed.core.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.vplayed.R
import com.vplayed.core.error.AppError
import com.vplayed.shared.core.error.AppError as SharedAppError

/** User-facing message for an [AppError]. */
@Composable
fun AppError.userMessage(): String = stringResource(userMessageRes())

/** String resource of [userMessage], for one-time messages (e.g. Toasts) built outside composition. */
@StringRes
fun AppError.userMessageRes(): Int = when (this) {
    AppError.Network -> R.string.error_network
    AppError.Unauthorized -> R.string.error_unauthorized
    is AppError.Server -> R.string.error_server
    AppError.Parsing -> R.string.error_parsing
    is AppError.Unknown -> R.string.error_unknown
}

/** User-facing message for a shared-layer error (Home and other shared features). */
@Composable
fun SharedAppError.userMessage(): String = stringResource(userMessageRes())

/** Shared errors use the same strings; a timeout reads as a network problem, as before the migration. */
@StringRes
fun SharedAppError.userMessageRes(): Int = when (this) {
    SharedAppError.Network, SharedAppError.Timeout -> R.string.error_network
    SharedAppError.Unauthorized -> R.string.error_unauthorized
    is SharedAppError.Server -> R.string.error_server
    SharedAppError.Parsing -> R.string.error_parsing
    is SharedAppError.Configuration, is SharedAppError.Unknown -> R.string.error_unknown
}
