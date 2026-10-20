package com.vplayed.feature.auth.presentation.viewmodel

import android.content.Context
import androidx.compose.runtime.Composable
import com.vplayed.core.ui.brandName
import com.vplayed.core.ui.sharedString
import com.vplayed.shared.auth.AuthMessage
import com.vplayed.shared.l10n.resource
import org.jetbrains.compose.resources.stringResource

/*
 * Text for the shared layer's sign-in messages ([AuthMessage]): the server's own words when safe to
 * show, else the shared catalog's text for its key (`AuthMessageKey.resource`, the mapping VPlayed
 * uses too). The choice between the two is shared (`AuthMessage.serverOr`).
 */

/** The text to show for [message] in a composable. */
@Composable
fun AuthMessage.Companion.textOf(message: AuthMessage): String = when (message) {
    is AuthMessage.Server -> message.text
    is AuthMessage.Localized -> stringResource(message.key.resource, brandName())
}

/** The text to show for this message outside composition (Toasts). */
fun AuthMessage.resolve(context: Context): String = when (this) {
    is AuthMessage.Server -> text
    is AuthMessage.Localized -> sharedString(key.resource, context.brandName())
}
