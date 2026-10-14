package com.vplayed.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vplayed.core.session.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** App-shell state: who is signed in (the side menu shows it instead of Login). */
@HiltViewModel
class AppShellViewModel @Inject constructor(sessions: SessionRepository) : ViewModel() {
    /** Email of the signed-in user, or null when signed out. */
    val signedInEmail: StateFlow<String?> = sessions.session
        .map { it?.email }
        .stateIn(viewModelScope, SharingStarted.Eagerly, sessions.session.value?.email)
}
