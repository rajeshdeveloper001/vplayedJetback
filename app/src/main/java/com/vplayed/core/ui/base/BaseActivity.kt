package com.vplayed.core.ui.base

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vplayed.core.designsystem.theme.AppTheme
import com.vplayed.core.designsystem.theme.isDark
import com.vplayed.core.locale.AppLocaleManager
import com.vplayed.shared.l10n.SharedLocalization

/**
 * Shared Activity behaviour: edge-to-edge, appearance-driven [AppTheme] and system bar styling.
 *
 * It is a [FragmentActivity] so Fragment screens (e.g. Login) can be hosted inside the Compose
 * NavHost with `AndroidFragment`; Navigation Compose stays the only navigation framework.
 * Business logic, API calls and app-wide network state do not belong here.
 */
abstract class BaseActivity : FragmentActivity() {

    protected val appearanceViewModel: AppearanceViewModel by viewModels()

    /** The app language picked in Menu → Language (applied here on Android 7–12; see [AppLocaleManager]). */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocaleManager.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // The shared layer's record of the language this activity shows (shared string catalog).
        SharedLocalization.setLanguage(resources.configuration.locales[0].toLanguageTag())
    }

    /**
     * Sets [content] wrapped in [AppTheme]. Nothing is drawn until the appearance preferences are
     * read, so the first frame always has the right theme.
     */
    protected fun setThemedContent(content: @Composable () -> Unit) {
        setContent {
            val uiState by appearanceViewModel.uiState.collectAsStateWithLifecycle()
            val appearance = (uiState as? AppearanceUiState.Ready)?.appearance ?: return@setContent
            val darkTheme = appearance.themeMode.isDark()

            // Keep status/navigation bar icons readable when the app theme differs from the system
            // theme (user override) or when the system theme changes while the app is open.
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(LIGHT_NAV_SCRIM, DARK_NAV_SCRIM) { darkTheme },
                )
                onDispose {}
            }

            AppTheme(appearance = appearance, darkTheme = darkTheme, content = content)
        }
    }

    private companion object {
        // Platform defaults for 3-button navigation scrims (androidx.activity EdgeToEdge).
        val LIGHT_NAV_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
        val DARK_NAV_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
    }
}
