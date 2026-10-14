package com.vplayed.feature.auth.presentation.viewmodel

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.vplayed.core.error.AppError
import com.vplayed.core.ui.userMessageRes

/** Text for a one-time message: the server's own words when safe to show, else an app string. */
sealed interface AuthMessage {
    data class Server(val text: String) : AuthMessage
    data class Resource(@param:StringRes val id: Int) : AuthMessage

    companion object {
        private const val MAX_SERVER_MESSAGE_LENGTH = 200

        /** [serverText] when it is short plain text (no markup, stack traces or multi-line dumps), else [fallback]. */
        fun serverOr(serverText: String?, @StringRes fallback: Int): AuthMessage =
            serverText?.trim()?.takeIf(::isDisplayable)?.let(::Server) ?: Resource(fallback)

        fun from(error: AppError): AuthMessage = Resource(error.userMessageRes())

        /** The text to show for [message] in a composable. */
        @Composable
        fun textOf(message: AuthMessage): String = when (message) {
            is Server -> message.text
            is Resource -> stringResource(message.id)
        }

        private fun isDisplayable(text: String): Boolean =
            text.isNotEmpty() && text.length <= MAX_SERVER_MESSAGE_LENGTH && text.none { it == '<' || it == '>' || it == '\n' }
    }
}
