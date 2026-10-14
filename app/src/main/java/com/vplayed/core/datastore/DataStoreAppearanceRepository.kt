package com.vplayed.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.vplayed.core.designsystem.dimension.UiDensity
import com.vplayed.core.designsystem.theme.AppearanceConfig
import com.vplayed.core.designsystem.theme.ThemeMode
import com.vplayed.core.designsystem.typography.TextScale
import com.vplayed.core.repository.AppearanceRepository
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Singleton
class DataStoreAppearanceRepository @Inject constructor(
    @param:AppearancePreferences private val dataStore: DataStore<Preferences>,
) : AppearanceRepository {

    override val appearance: Flow<AppearanceConfig> = dataStore.data
        // A corrupt or unreadable file must not crash the app; fall back to defaults.
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map(::toAppearanceConfig)
        .distinctUntilChanged()

    override suspend fun setThemeMode(mode: ThemeMode) = edit { it[Keys.THEME_MODE] = mode.name }

    override suspend fun setPalette(paletteId: String) = edit { it[Keys.PALETTE] = paletteId }

    override suspend fun setFont(fontId: String) = edit { it[Keys.FONT] = fontId }

    override suspend fun setTextScale(scale: Float) = edit { it[Keys.TEXT_SCALE] = TextScale.coerce(scale) }

    override suspend fun setDensity(density: UiDensity) = edit { it[Keys.DENSITY] = density.name }

    override suspend fun setDynamicColor(enabled: Boolean) = edit { it[Keys.DYNAMIC_COLOR] = enabled }

    override suspend fun reset() = edit { it.clear() }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        dataStore.edit(block)
    }

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val PALETTE = stringPreferencesKey("palette_id")
        val FONT = stringPreferencesKey("font_id")
        val TEXT_SCALE = floatPreferencesKey("text_scale")
        val DENSITY = stringPreferencesKey("density")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
    }

    internal companion object {
        /** Maps stored preferences to a config; missing or unknown values fall back to defaults. */
        fun toAppearanceConfig(prefs: Preferences): AppearanceConfig {
            val defaults = AppearanceConfig.Default
            return AppearanceConfig(
                themeMode = prefs[Keys.THEME_MODE].toEnumOrNull<ThemeMode>() ?: defaults.themeMode,
                paletteId = prefs[Keys.PALETTE] ?: defaults.paletteId,
                fontId = prefs[Keys.FONT] ?: defaults.fontId,
                textScale = prefs[Keys.TEXT_SCALE]?.let(TextScale::coerce) ?: defaults.textScale,
                density = prefs[Keys.DENSITY].toEnumOrNull<UiDensity>() ?: defaults.density,
                dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: defaults.dynamicColor,
            )
        }

        private inline fun <reified T : Enum<T>> String?.toEnumOrNull(): T? =
            this?.let { name -> enumValues<T>().firstOrNull { it.name == name } }
    }
}
