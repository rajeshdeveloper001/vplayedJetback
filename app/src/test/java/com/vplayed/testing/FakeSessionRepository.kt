package com.vplayed.testing

import com.vplayed.core.session.SessionRepository
import com.vplayed.core.session.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** In-memory [SessionRepository]; [saved] records every saved session. */
class FakeSessionRepository(initial: UserSession? = null) : SessionRepository {
    private val _session = MutableStateFlow(initial)
    override val session: StateFlow<UserSession?> = _session.asStateFlow()
    val saved = mutableListOf<UserSession>()

    override suspend fun awaitLoaded() = Unit

    override suspend fun save(session: UserSession) {
        saved += session
        _session.value = session
    }
}
