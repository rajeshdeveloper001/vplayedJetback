package com.vplayed.testing

import com.vplayed.core.network.NetworkModule
import kotlinx.serialization.json.Json

/** Test helpers for JSON. The Home and geo fixtures moved to ../kmp with the code they test (Phase 4). */
object Fixtures {
    /** The app's real JSON configuration, so tests parse exactly like production. */
    val json: Json = NetworkModule.provideJson()
}
