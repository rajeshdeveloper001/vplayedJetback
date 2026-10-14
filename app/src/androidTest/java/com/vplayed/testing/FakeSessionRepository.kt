package com.vplayed.testing

import com.vplayed.core.session.SessionRepository
import com.vplayed.core.session.UserSession
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** In-memory session: instrumented flows never touch the Keystore or storage. */
@Singleton
class FakeSessionRepository @Inject constructor() : SessionRepository {
    private val _session = MutableStateFlow<UserSession?>(null)
    override val session: StateFlow<UserSession?> = _session.asStateFlow()

    override suspend fun awaitLoaded() = Unit

    override suspend fun save(session: UserSession) {
        _session.value = session
    }
}
