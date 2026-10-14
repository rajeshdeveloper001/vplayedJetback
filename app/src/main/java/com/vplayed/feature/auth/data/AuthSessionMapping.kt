package com.vplayed.feature.auth.data

import com.vplayed.core.session.UserSession
import com.vplayed.shared.auth.AuthSession

/**
 * The app's stored session for a shared [AuthSession] (the app owns the session until Phase 5).
 * Without an account email or id, the email the user signed in with identifies the user.
 */
fun AuthSession.toUserSession(requestedEmail: String): UserSession {
    val userEmail = email ?: requestedEmail
    return UserSession(
        userId = userId ?: userEmail,
        email = userEmail,
        displayName = firstName,
        accessToken = accessToken,
        refreshToken = refreshToken,
        profileId = profileId,
    )
}
