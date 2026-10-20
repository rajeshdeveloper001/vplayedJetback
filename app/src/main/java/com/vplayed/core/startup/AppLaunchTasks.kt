package com.vplayed.core.startup

import com.vplayed.core.dispatcher.ApplicationScope
import com.vplayed.shared.SharedGraph
import com.vplayed.shared.core.error.DataResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Launch-time work of the previous app (`SceneDelegate.getCommonKeys` + `checkIfUserLoggedIn`):
 * loads the common settings into the shared `CommonSettingsStore` (login method, guest access), the
 * language list into the shared `SupportedLanguagesStore` (Menu → Language), and starts the shared
 * automatic guest session, which keeps a guest session whenever nobody is signed
 * in and the settings allow guests. Started from `VPlayedApplication`, so instrumented tests (on
 * `HiltTestApplication`) make no such calls.
 */
@Singleton
class AppLaunchTasks @Inject constructor(
    private val graph: SharedGraph,
    @ApplicationScope private val scope: CoroutineScope,
) {
    fun start() {
        graph.guestSessionKeeper.start()
        scope.launch {
            // A failure keeps the defaults (email sign-in, no guest session), as in the previous app.
            if (graph.commonSettingsRepository.load() is DataResult.Failure) {
                graph.logger.info(TAG, "Common settings could not be loaded")
            }
        }
        scope.launch {
            // A failure keeps the built-in list (the languages the app has translations for).
            if (graph.languageRepository.load() is DataResult.Failure) {
                graph.logger.info(TAG, "Language list could not be loaded")
            }
        }
    }

    private companion object {
        const val TAG = "AppLaunch"
    }
}
