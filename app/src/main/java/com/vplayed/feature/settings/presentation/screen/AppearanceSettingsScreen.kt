package com.vplayed.feature.settings.presentation.screen

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vplayed.R
import com.vplayed.core.designsystem.component.AppButton
import com.vplayed.core.designsystem.component.AppButtonStyle
import com.vplayed.core.designsystem.component.AppCard
import com.vplayed.core.designsystem.component.AppLoadingIndicator
import com.vplayed.core.designsystem.dimension.UiDensity
import com.vplayed.core.designsystem.theme.AppDesignSystem
import com.vplayed.core.designsystem.theme.AppTheme
import com.vplayed.core.designsystem.theme.AppearanceConfig
import com.vplayed.core.designsystem.theme.BrandConfig
import com.vplayed.core.designsystem.theme.ThemeMode
import com.vplayed.core.designsystem.theme.isDark
import com.vplayed.core.designsystem.typography.TextScale
import com.vplayed.core.ui.AppScreenScaffold
import com.vplayed.core.ui.adaptive.ResponsiveContent
import com.vplayed.core.ui.plus
import com.vplayed.core.ui.preview.ScreenPreviews
import com.vplayed.feature.settings.presentation.viewmodel.AppearanceSettingsUiState
import com.vplayed.feature.settings.presentation.viewmodel.AppearanceSettingsViewModel
import kotlin.math.roundToInt

/** All user actions on the appearance screen. */
data class AppearanceActions(
    val onThemeModeSelected: (ThemeMode) -> Unit = {},
    val onPaletteSelected: (String) -> Unit = {},
    val onFontSelected: (String) -> Unit = {},
    val onTextScaleChanged: (Float) -> Unit = {},
    val onDensitySelected: (UiDensity) -> Unit = {},
    val onDynamicColorChanged: (Boolean) -> Unit = {},
    val onReset: () -> Unit = {},
    val onNavigateBack: (() -> Unit)? = null,
    val onOpenDesignReference: (() -> Unit)? = null,
)

@Composable
fun AppearanceSettingsRoute(
    onNavigateBack: () -> Unit,
    onOpenDesignReference: () -> Unit,
    viewModel: AppearanceSettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val actions = remember(viewModel, onNavigateBack, onOpenDesignReference) {
        AppearanceActions(
            onNavigateBack = onNavigateBack,
            onOpenDesignReference = onOpenDesignReference,
            onThemeModeSelected = viewModel::onThemeModeSelected,
            onPaletteSelected = viewModel::onPaletteSelected,
            onFontSelected = viewModel::onFontSelected,
            onTextScaleChanged = viewModel::onTextScaleChanged,
            onDensitySelected = viewModel::onDensitySelected,
            onDynamicColorChanged = viewModel::onDynamicColorChanged,
            onReset = viewModel::onResetClicked,
        )
    }
    AppearanceSettingsScreen(uiState = uiState, actions = actions)
}

@Composable
fun AppearanceSettingsScreen(
    uiState: AppearanceSettingsUiState,
    actions: AppearanceActions,
    modifier: Modifier = Modifier,
    supportsDynamicColor: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
) {
    AppScreenScaffold(
        title = stringResource(R.string.appearance_title),
        modifier = modifier,
        onNavigateBack = actions.onNavigateBack,
    ) { padding ->
        when (uiState) {
            AppearanceSettingsUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                AppLoadingIndicator()
            }
            is AppearanceSettingsUiState.Ready -> {
                val dimens = AppDesignSystem.dimens
                val config = uiState.appearance
                ResponsiveContent(modifier = Modifier.fillMaxSize()) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = dimens.adaptiveGridMinCellWidth),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = padding.plus(dimens.screenHorizontalPadding, dimens.screenVerticalPadding),
                        horizontalArrangement = Arrangement.spacedBy(dimens.cardSpacing),
                        verticalArrangement = Arrangement.spacedBy(dimens.cardSpacing),
                    ) {
                        item(key = "theme") { ThemeSection(config, supportsDynamicColor, actions) }
                        item(key = "palette") { PaletteSection(config, actions.onPaletteSelected) }
                        if (BrandConfig.fonts.size > 1) {
                            item(key = "font") { FontSection(config.fontId, actions.onFontSelected) }
                        }
                        item(key = "size") { SizeSection(config, actions) }
                        item(key = "reset", span = { GridItemSpan(maxLineSpan) }) {
                            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                AppButton(
                                    text = stringResource(R.string.appearance_reset),
                                    onClick = actions.onReset,
                                    style = AppButtonStyle.SECONDARY,
                                )
                                actions.onOpenDesignReference?.let { open ->
                                    AppButton(
                                        text = stringResource(R.string.appearance_open_design_reference),
                                        onClick = open,
                                        style = AppButtonStyle.TEXT,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = AppDesignSystem.typography.titleMedium,
        color = AppDesignSystem.colors.primary,
        modifier = Modifier.semantics { heading() },
    )
}

@Composable
private fun ThemeSection(config: AppearanceConfig, supportsDynamicColor: Boolean, actions: AppearanceActions) {
    val modes = ThemeMode.entries
    AppCard(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(stringResource(R.string.appearance_section_theme))
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = AppDesignSystem.dimens.minTouchTarget),
        ) {
            modes.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = config.themeMode == mode,
                    onClick = { actions.onThemeModeSelected(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                    label = { Text(stringResource(mode.labelRes), maxLines = 1) },
                )
            }
        }
        if (supportsDynamicColor) {
            SwitchRow(
                label = stringResource(R.string.appearance_dynamic_color),
                checked = config.dynamicColor,
                onCheckedChange = actions.onDynamicColorChanged,
            )
        }
    }
}

@Composable
private fun PaletteSection(config: AppearanceConfig, onSelected: (String) -> Unit) {
    val dark = config.themeMode.isDark()
    AppCard(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(stringResource(R.string.appearance_section_palette))
        if (config.dynamicColor) {
            Text(
                stringResource(R.string.appearance_palette_dynamic_note),
                style = AppDesignSystem.typography.bodySmall,
                color = AppDesignSystem.colors.onSurfaceVariant,
            )
        }
        Column(Modifier.selectableGroup()) {
            BrandConfig.palettes.forEach { palette ->
                val scheme = if (dark) palette.darkScheme else palette.lightScheme
                RadioRow(
                    label = stringResource(palette.nameRes),
                    selected = BrandConfig.palette(config.paletteId).id == palette.id,
                    onClick = { onSelected(palette.id) },
                ) {
                    Box(
                        Modifier
                            .size(AppDesignSystem.dimens.icon.medium)
                            .background(scheme.primary, CircleShape),
                    )
                }
            }
        }
    }
}

@Composable
private fun FontSection(selectedFontId: String, onSelected: (String) -> Unit) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(stringResource(R.string.appearance_section_font))
        Column(Modifier.selectableGroup()) {
            BrandConfig.fonts.forEach { font ->
                RadioRow(
                    label = stringResource(font.nameRes),
                    labelFontFamily = font.fontFamily,
                    selected = BrandConfig.font(selectedFontId).id == font.id,
                    onClick = { onSelected(font.id) },
                )
            }
        }
    }
}

@Composable
private fun SizeSection(config: AppearanceConfig, actions: AppearanceActions) {
    // Local state while dragging; persisted once when the drag ends to avoid a write per frame.
    var sliderValue by remember(config.textScale) { mutableFloatStateOf(config.textScale) }
    val percent = (sliderValue * PERCENT).roundToInt()
    val sliderDescription = stringResource(R.string.appearance_text_size_value, percent)
    val densities = UiDensity.entries
    AppCard(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(stringResource(R.string.appearance_section_size))
        Text(sliderDescription, style = AppDesignSystem.typography.bodyMedium)
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = { actions.onTextScaleChanged(sliderValue) },
            valueRange = TextScale.MIN..TextScale.MAX,
            steps = TEXT_SCALE_STEPS,
            modifier = Modifier.semantics { contentDescription = sliderDescription },
        )
        Text(stringResource(R.string.appearance_density), style = AppDesignSystem.typography.bodyMedium)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            densities.forEachIndexed { index, density ->
                SegmentedButton(
                    selected = config.density == density,
                    onClick = { actions.onDensitySelected(density) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = densities.size),
                    label = { Text(stringResource(density.labelRes), maxLines = 1) },
                )
            }
        }
    }
}

@Composable
private fun RadioRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    labelFontFamily: FontFamily? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val dimens = AppDesignSystem.dimens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = dimens.listItemHeight)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.spacing.sm),
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = label,
            style = AppDesignSystem.typography.bodyLarge.let { style ->
                if (labelFontFamily != null) style.copy(fontFamily = labelFontFamily) else style
            },
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke()
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = AppDesignSystem.dimens.listItemHeight)
            .toggleable(value = checked, onValueChange = onCheckedChange, role = Role.Switch),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = AppDesignSystem.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null)
    }
}

private const val PERCENT = 100
/** 0.85..1.30 in 0.05 increments → 8 intermediate stops. */
private const val TEXT_SCALE_STEPS = 8

private val ThemeMode.labelRes: Int
    get() = when (this) {
        ThemeMode.SYSTEM -> R.string.theme_mode_system
        ThemeMode.LIGHT -> R.string.theme_mode_light
        ThemeMode.DARK -> R.string.theme_mode_dark
    }

private val UiDensity.labelRes: Int
    get() = when (this) {
        UiDensity.COMFORTABLE -> R.string.density_comfortable
        UiDensity.COMPACT -> R.string.density_compact
    }

@ScreenPreviews
@Composable
private fun AppearanceSettingsScreenPreview() {
    AppTheme {
        AppearanceSettingsScreen(
            uiState = AppearanceSettingsUiState.Ready(AppearanceConfig.Default),
            actions = AppearanceActions(),
            supportsDynamicColor = true,
        )
    }
}
