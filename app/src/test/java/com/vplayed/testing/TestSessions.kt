package com.vplayed.testing

import com.vplayed.shared.auth.AuthSession
import com.vplayed.shared.session.InMemorySecureStore
import com.vplayed.shared.session.SessionManager
import kotlinx.coroutines.runBlocking

/** The shared [SessionManager] over memory (no Keystore, no files), already loaded; signed in when [signedIn] is given. */
fun testSessionManager(signedIn: AuthSession? = null): SessionManager = SessionManager(InMemorySecureStore()).also { sessions ->
    runBlocking { if (signedIn != null) sessions.signIn(signedIn) else sessions.awaitLoaded() }
}
