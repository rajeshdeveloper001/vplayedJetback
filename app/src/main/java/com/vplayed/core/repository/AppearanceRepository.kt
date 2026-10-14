package com.vplayed.core.repository

import com.vplayed.core.designsystem.dimension.UiDensity
import com.vplayed.core.designsystem.theme.AppearanceConfig
import com.vplayed.core.designsystem.theme.ThemeMode
import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth for the user's appearance preferences. ViewModels observe [appearance]
 * and never keep their own copy.
 */
interface AppearanceRepository {
    val appearance: Flow<AppearanceConfig>

    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setPalette(paletteId: String)
    suspend fun setFont(fontId: String)
    suspend fun setTextScale(scale: Float)
    suspend fun setDensity(density: UiDensity)
    suspend fun setDynamicColor(enabled: Boolean)
    suspend fun reset()
}
