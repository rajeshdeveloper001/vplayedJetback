package com.vplayed.testing

import com.vplayed.core.designsystem.dimension.UiDensity
import com.vplayed.core.designsystem.theme.AppearanceConfig
import com.vplayed.core.designsystem.theme.ThemeMode
import com.vplayed.core.repository.AppearanceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** In-memory [AppearanceRepository] for ViewModel tests. */
class FakeAppearanceRepository(initial: AppearanceConfig = AppearanceConfig.Default) : AppearanceRepository {
    val state = MutableStateFlow(initial)
    override val appearance: Flow<AppearanceConfig> = state

    override suspend fun setThemeMode(mode: ThemeMode) = state.update { it.copy(themeMode = mode) }
    override suspend fun setPalette(paletteId: String) = state.update { it.copy(paletteId = paletteId) }
    override suspend fun setFont(fontId: String) = state.update { it.copy(fontId = fontId) }
    override suspend fun setTextScale(scale: Float) = state.update { it.copy(textScale = scale) }
    override suspend fun setDensity(density: UiDensity) = state.update { it.copy(density = density) }
    override suspend fun setDynamicColor(enabled: Boolean) = state.update { it.copy(dynamicColor = enabled) }
    override suspend fun reset() = state.update { AppearanceConfig.Default }
}
