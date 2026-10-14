package com.vplayed.core.datastore

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.vplayed.core.designsystem.dimension.UiDensity
import com.vplayed.core.designsystem.theme.AppearanceConfig
import com.vplayed.core.designsystem.theme.ThemeMode
import com.vplayed.core.designsystem.typography.TextScale
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreAppearanceRepositoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val testScope = TestScope(UnconfinedTestDispatcher())
    private val dataStore = PreferenceDataStoreFactory.create(
        scope = testScope.backgroundScope,
        produceFile = { File(tmp.root, "appearance_test.preferences_pb") },
    )
    private val repository = DataStoreAppearanceRepository(dataStore)

    @Test
    fun givenNoStoredPreferences_whenReading_thenReturnsDefaults() = testScope.runTest {
        assertEquals(AppearanceConfig.Default, repository.appearance.first())
    }

    @Test
    fun givenThemeAndPaletteChanged_whenReading_thenReturnsPersistedValues() = testScope.runTest {
        repository.setThemeMode(ThemeMode.DARK)
        repository.setPalette("monochrome")
        repository.setDensity(UiDensity.COMPACT)
        repository.setDynamicColor(true)

        val config = repository.appearance.first()
        assertEquals(ThemeMode.DARK, config.themeMode)
        assertEquals("monochrome", config.paletteId)
        assertEquals(UiDensity.COMPACT, config.density)
        assertEquals(true, config.dynamicColor)
    }

    @Test
    fun givenTextScaleOutOfRange_whenSaving_thenClampsToAccessibleBounds() = testScope.runTest {
        repository.setTextScale(5f)
        assertEquals(TextScale.MAX, repository.appearance.first().textScale)

        repository.setTextScale(0.1f)
        assertEquals(TextScale.MIN, repository.appearance.first().textScale)
    }

    @Test
    fun givenUnknownStoredEnum_whenReading_thenFallsBackToDefault() = testScope.runTest {
        dataStore.edit { it[stringPreferencesKey("theme_mode")] = "SEPIA" }
        assertEquals(ThemeMode.SYSTEM, repository.appearance.first().themeMode)
    }

    @Test
    fun givenCustomizedAppearance_whenReset_thenReturnsDefaults() = testScope.runTest {
        repository.setThemeMode(ThemeMode.LIGHT)
        repository.setTextScale(1.2f)

        repository.reset()

        assertEquals(AppearanceConfig.Default, repository.appearance.first())
    }
}
