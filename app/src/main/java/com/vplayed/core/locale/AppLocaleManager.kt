package com.vplayed.core.locale

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.core.content.edit
import com.vplayed.shared.l10n.SharedLocalization
import java.util.Locale

/**
 * The app language the user picked (Menu → Language), applied as the per-app locale. App text comes
 * from the shared string catalog and follows it, as do `X-Language-Code` (`Locale.getDefault()`) and
 * the layout direction.
 *
 * - Android 13+: the framework per-app locale, the same setting as Settings → Apps → VPlayed →
 *   Language (`res/xml/locales_config.xml`). The system stores it and recreates the activity.
 * - Android 7–12: stored here and applied to each activity's context in [wrap] (called from
 *   `BaseActivity.attachBaseContext`), then the activity is recreated. Stored in SharedPreferences
 *   because it must be read synchronously before the first frame.
 *
 * With no choice the app follows the system languages.
 */
object AppLocaleManager {
    private const val PREFERENCES = "app_locale"
    private const val KEY_LANGUAGE = "language"

    /** The chosen language code, or null when the app follows the system. */
    fun savedLanguage(context: Context): String? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java).applicationLocales.takeUnless { it.isEmpty }?.get(0)?.language
        } else {
            context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).getString(KEY_LANGUAGE, null)
        }

    /** The language tags to resolve the app language from, most preferred first: the choice, then the system's. */
    fun preferredLanguages(context: Context): List<String> {
        val system = LocaleList.getDefault()
        return listOfNotNull(savedLanguage(context)) + (0 until system.size()).map { system[it].toLanguageTag() }
    }

    /** Switches the app to [code] and re-creates [activity] in it. */
    fun setLanguage(activity: Activity, code: String) {
        SharedLocalization.setLanguage(code)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.getSystemService(LocaleManager::class.java).applicationLocales = LocaleList.forLanguageTags(code)
        } else {
            activity.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit { putString(KEY_LANGUAGE, code) }
            activity.recreate()
        }
    }

    /** [base] in the chosen language on Android 7–12 (newer versions apply it themselves). */
    fun wrap(base: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return base
        val code = savedLanguage(base) ?: return base
        val locale = Locale.forLanguageTag(code)
        Locale.setDefault(locale)
        val configuration = Configuration(base.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        return base.createConfigurationContext(configuration)
    }
}
