package com.vplayed.core.shared

import com.vplayed.core.geo.GeoRestrictionStore
import com.vplayed.core.session.SessionRepository
import com.vplayed.shared.core.network.AuthTokens
import com.vplayed.shared.core.network.RequestCredentials
import javax.inject.Inject

/**
 * Credentials for shared-layer requests, read live from the same stores `ApiHeadersInterceptor`
 * reads, so both stacks always send the same session and geo token. The app keeps owning them
 * until the shared session manager exists (Phase 5).
 */
class AndroidRequestCredentials @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val geoRestrictionStore: GeoRestrictionStore,
) : RequestCredentials {
    override val authTokens: AuthTokens?
        get() = sessionRepository.session.value?.let { AuthTokens(it.accessToken, it.profileId) }

    override val geoToken: String? get() = geoRestrictionStore.state.value.geoToken
}
