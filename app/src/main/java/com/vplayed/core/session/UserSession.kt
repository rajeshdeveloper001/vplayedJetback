package com.vplayed.core.session

import kotlinx.serialization.Serializable

/**
 * The signed-in user, as returned by a successful email-OTP verification. The tokens are
 * credentials: [toString] never prints them, and they are never logged.
 */
@Serializable
data class UserSession(
    val userId: String,
    val email: String,
    val displayName: String? = null,
    val accessToken: String,
    val refreshToken: String? = null,
    /** Id of the default profile; sent as the `profile_id` header. */
    val profileId: String? = null,
) {
    override fun toString(): String = "UserSession(userId=$userId, email=***, tokens=***)"
}
