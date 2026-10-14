package com.vplayed.core.session

import kotlinx.coroutines.flow.StateFlow

/** Single source of truth for the signed-in user. */
interface SessionRepository {
    /** The current session, or null when signed out. Stays null until [awaitLoaded] completes. */
    val session: StateFlow<UserSession?>

    /** Suspends until the stored session (if any) has been read, so first requests carry its headers. */
    suspend fun awaitLoaded()

    suspend fun save(session: UserSession)
}
