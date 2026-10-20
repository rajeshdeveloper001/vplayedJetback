package com.vplayed.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vplayed.core.ui.userMessageRes
import com.vplayed.shared.auth.AuthRepository
import com.vplayed.shared.auth.LogoutResult
import com.vplayed.shared.l10n.Res
import com.vplayed.shared.l10n.menu_sign_out_failed
import com.vplayed.shared.session.SessionManager
import com.vplayed.shared.session.SessionStatus
import com.vplayed.shared.session.SignOutReason
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource

/** App-shell state: who is signed in (the side menu shows it instead of Login) and signing out. */
@HiltViewModel
class AppShellViewModel @Inject constructor(
    private val sessions: SessionManager,
    private val authRepository: AuthRepository,
) : ViewModel() {
    /**
     * How the menu names the signed-in account (email, else phone number, else first name), or null
     * when signed out or a guest: guests still see Login, as in the previous app.
     */
    val signedInEmail: StateFlow<String?> = sessions.status
        .map { it.signedInEmail }
        .stateIn(viewModelScope, SharingStarted.Eagerly, sessions.status.value.signedInEmail)

    /**
     * Emits when the server ended a signed-in account's session (refused token and refresh): the app
     * says so and opens Login (previous app: "Session Expired, Please login to continue"). Guests get a
     * new guest session silently.
     */
    val sessionExpired: Flow<Unit> = sessions.status
        .runningFold(null as Pair<SessionStatus?, SessionStatus>?) { previous, current -> previous?.second to current }
        .filterNotNull()
        .map { (previous, current) ->
            val wasAccount = (previous as? SessionStatus.SignedIn)?.session?.isGuest == false
            val expired = (current as? SessionStatus.SignedOut)?.reason == SignOutReason.EXPIRED
            wasAccount && expired
        }
        .filter { it }
        .map { }

    private val _isSigningOut = MutableStateFlow(false)

    /** A sign-out request is in flight; the menu entry says so and ignores taps. */
    val isSigningOut: StateFlow<Boolean> = _isSigningOut.asStateFlow()

    private val _messages = Channel<StringResource>(Channel.BUFFERED)

    /** One-time messages (shared catalog strings, formatted with the brand name) for a Toast: why signing out failed. */
    val messages: Flow<StringResource> = _messages.receiveAsFlow()

    /**
     * Logs out through the shared rule (`SessionManager.logout`): the session ends when the server
     * confirms or no longer knows the token; other failures keep it and explain why.
     */
    fun signOut() {
        if (_isSigningOut.value) return
        _isSigningOut.value = true
        viewModelScope.launch {
            try {
                failureMessage(sessions.logout(authRepository))?.let { _messages.send(it) }
            } finally {
                _isSigningOut.value = false
            }
        }
    }

    private fun failureMessage(result: LogoutResult): StringResource? = when (result) {
        LogoutResult.LoggedOut, LogoutResult.SessionAlreadyEnded -> null
        // The server's own words are not shown: they are not vetted for this screen.
        is LogoutResult.Rejected -> Res.string.menu_sign_out_failed
        is LogoutResult.Failure -> result.error.userMessageRes()
    }

    private val SessionStatus.signedInEmail: String?
        get() = (this as? SessionStatus.SignedIn)?.session?.takeUnless { it.isGuest }
            ?.let { it.email ?: it.phone ?: it.firstName.orEmpty() }
}
