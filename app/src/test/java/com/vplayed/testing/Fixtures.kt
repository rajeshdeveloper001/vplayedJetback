package com.vplayed.testing

import com.vplayed.core.network.NetworkModule
import kotlinx.serialization.json.Json

/** Synthetic API responses from `src/test/resources` (no real user or studio data). */
object Fixtures {
    /** The app's real JSON configuration, so tests parse exactly like production. */
    val json: Json = NetworkModule.provideJson()

    fun read(path: String): String =
        requireNotNull(javaClass.classLoader?.getResource(path)) { "Missing fixture $path" }.readText()
}
