package com.vplayed.app

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vplayed.R
import com.vplayed.core.ui.userMessageRes
import com.vplayed.shared.auth.AuthRepository
import com.vplayed.shared.auth.LogoutResult
import com.vplayed.shared.session.SessionManager
import com.vplayed.shared.session.SessionStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** App-shell state: who is signed in (the side menu shows it instead of Login) and signing out. */
@HiltViewModel
class AppShellViewModel @Inject constructor(
    private val sessions: SessionManager,
    private val authRepository: AuthRepository,
) : ViewModel() {
    /** Email of the signed-in user, or null when signed out (or the session ended). */
    val signedInEmail: StateFlow<String?> = sessions.status
        .map { it.signedInEmail }
        .stateIn(viewModelScope, SharingStarted.Eagerly, sessions.status.value.signedInEmail)

    private val _isSigningOut = MutableStateFlow(false)

    /** A sign-out request is in flight; the menu entry says so and ignores taps. */
    val isSigningOut: StateFlow<Boolean> = _isSigningOut.asStateFlow()

    private val _messages = Channel<Int>(Channel.BUFFERED)

    /** One-time messages (string resources) for a Toast: why signing out failed. */
    val messages: Flow<Int> = _messages.receiveAsFlow()

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

    @StringRes
    private fun failureMessage(result: LogoutResult): Int? = when (result) {
        LogoutResult.LoggedOut, LogoutResult.SessionAlreadyEnded -> null
        // The server's own words are not shown: they are not vetted for this screen.
        is LogoutResult.Rejected -> R.string.menu_sign_out_failed
        is LogoutResult.Failure -> result.error.userMessageRes()
    }

    private val SessionStatus.signedInEmail: String?
        get() = (this as? SessionStatus.SignedIn)?.session?.email
}
