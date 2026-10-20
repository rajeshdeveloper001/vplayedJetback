package com.vplayed.core.ui

import androidx.compose.runtime.Composable
import com.vplayed.shared.l10n.messageResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import com.vplayed.shared.core.error.AppError as SharedAppError

/** User-facing message for a shared-layer error (Home, sign-out and other shared features), from the shared catalog. */
@Composable
fun SharedAppError.userMessage(): String = stringResource(userMessageRes(), brandName())

/** The catalog string of [userMessage], for one-time messages (e.g. Toasts) built outside composition; format it with the brand name. */
fun SharedAppError.userMessageRes(): StringResource = messageResource
