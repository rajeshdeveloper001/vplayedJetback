package com.vplayed.reference.showcase.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vplayed.R
import com.vplayed.core.common.validation.EmailValidationResult
import com.vplayed.core.designsystem.component.AppButton
import com.vplayed.core.designsystem.component.AppButtonStyle
import com.vplayed.core.designsystem.component.AppCard
import com.vplayed.core.designsystem.component.AppDialog
import com.vplayed.core.designsystem.component.AppEmptyState
import com.vplayed.core.designsystem.component.AppErrorView
import com.vplayed.core.designsystem.component.AppLoadingIndicator
import com.vplayed.core.designsystem.component.AppTextField
import com.vplayed.core.designsystem.icon.AppIcons
import com.vplayed.core.designsystem.theme.AppDesignSystem
import com.vplayed.core.designsystem.theme.AppTheme
import com.vplayed.core.ui.AppScreenScaffold
import com.vplayed.core.ui.adaptive.ResponsiveContent
import com.vplayed.core.ui.plus
import com.vplayed.core.ui.preview.ScreenPreviews
import com.vplayed.reference.showcase.presentation.components.ColorSwatch
import com.vplayed.reference.showcase.presentation.components.SectionTitle
import com.vplayed.reference.showcase.presentation.viewmodel.DemoContentState
import com.vplayed.reference.showcase.presentation.viewmodel.DesignShowcaseUiState
import com.vplayed.reference.showcase.presentation.viewmodel.DesignShowcaseViewModel
import com.vplayed.reference.showcase.presentation.viewmodel.EnvironmentInfo
import com.vplayed.reference.typography.OttTextStylesReference

/** Stateful entry point used by navigation. */
@Composable
fun DesignShowcaseRoute(onNavigateBack: () -> Unit, viewModel: DesignShowcaseViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DesignShowcaseScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onEmailChange = viewModel::onEmailChange,
        onSubmitEmail = viewModel::onSubmitEmail,
        onDemoStateSelected = viewModel::onDemoStateSelected,
    )
}

/**
 * Design-system showcase. Sections are cards in an adaptive grid: one column on phones in
 * portrait, two or more in landscape and on tablets.
 */
@Composable
fun DesignShowcaseScreen(
    uiState: DesignShowcaseUiState,
    onEmailChange: (String) -> Unit,
    onSubmitEmail: () -> Unit,
    onDemoStateSelected: (DemoContentState) -> Unit,
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null,
) {
    val dimens = AppDesignSystem.dimens
    AppScreenScaffold(
        title = stringResource(R.string.showcase_title),
        modifier = modifier,
        onNavigateBack = onNavigateBack,
    ) { padding ->
        ResponsiveContent(modifier = Modifier.fillMaxSize()) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = dimens.adaptiveGridMinCellWidth),
                modifier = Modifier.fillMaxSize().testTag(ShowcaseTestTags.GRID),
                contentPadding = padding.plus(dimens.screenHorizontalPadding, dimens.screenVerticalPadding),
                horizontalArrangement = Arrangement.spacedBy(dimens.cardSpacing),
                verticalArrangement = Arrangement.spacedBy(dimens.cardSpacing),
            ) {
                item(key = "environment", span = { GridItemSpan(maxLineSpan) }) {
                    EnvironmentCard(uiState.environment)
                }
                item(key = "colors") { ColorsCard() }
                item(key = "typography") { TypographyCard() }
                item(key = "components") {
                    ComponentsCard(
                        email = uiState.email,
                        emailError = uiState.emailError,
                        emailAccepted = uiState.emailAccepted,
                        onEmailChange = onEmailChange,
                        onSubmitEmail = onSubmitEmail,
                    )
                }
                item(key = "states") {
                    StatesCard(selected = uiState.demoState, onSelected = onDemoStateSelected)
                }
            }
        }
    }
}

/** Keeps the demo panel from jumping in height when switching states. */
private val StatePanelMinHeight = 240.dp

object ShowcaseTestTags {
    const val GRID = "showcase_grid"
    const val STATE_PANEL = "showcase_state_panel"
}

@Composable
private fun EnvironmentCard(info: EnvironmentInfo) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        SectionTitle(stringResource(R.string.showcase_section_environment))
        Text(
            text = stringResource(R.string.showcase_environment_value, info.environmentId, info.versionName),
            style = AppDesignSystem.typography.bodyLarge,
        )
        Text(
            text = info.serverUrl,
            style = AppDesignSystem.typography.bodyMedium,
            color = AppDesignSystem.colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun ColorsCard() {
    val colors = AppDesignSystem.colors
    val extended = AppDesignSystem.extendedColors
    AppCard(modifier = Modifier.fillMaxWidth()) {
        SectionTitle(stringResource(R.string.showcase_section_colors))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(AppDesignSystem.dimens.spacing.xs),
            verticalArrangement = Arrangement.spacedBy(AppDesignSystem.dimens.spacing.xs),
        ) {
            ColorSwatch("primary", colors.primary, colors.onPrimary)
            ColorSwatch("secondary", colors.secondary, colors.onSecondary)
            ColorSwatch("tertiary", colors.tertiary, colors.onTertiary)
            ColorSwatch("container", colors.primaryContainer, colors.onPrimaryContainer)
            ColorSwatch("surface", colors.surfaceVariant, colors.onSurfaceVariant)
            ColorSwatch("error", colors.error, colors.onError)
            ColorSwatch("success", extended.success, extended.onSuccess)
            ColorSwatch("warning", extended.warning, extended.onWarning)
            ColorSwatch("info", extended.info, extended.onInfo)
        }
    }
}

/** Shows the OTT text-style reference sheet (see `com.vplayed.reference.typography`). */
@Composable
private fun TypographyCard() {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        SectionTitle(stringResource(R.string.showcase_section_typography))
        OttTextStylesReference()
    }
}

@Composable
private fun ComponentsCard(
    email: String,
    emailError: EmailValidationResult?,
    emailAccepted: Boolean,
    onEmailChange: (String) -> Unit,
    onSubmitEmail: () -> Unit,
) {
    val spacing = AppDesignSystem.dimens.spacing
    var showDialog by rememberSaveable { mutableStateOf(false) }
    AppCard(modifier = Modifier.fillMaxWidth()) {
        SectionTitle(stringResource(R.string.showcase_section_components))
        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            AppTextField(
                value = email,
                onValueChange = onEmailChange,
                label = stringResource(R.string.showcase_email_label),
                supportingText = if (emailAccepted) stringResource(R.string.showcase_email_accepted) else null,
                errorMessage = when (emailError) {
                    EmailValidationResult.EMPTY -> stringResource(R.string.showcase_email_error_empty)
                    EmailValidationResult.INVALID_FORMAT -> stringResource(R.string.showcase_email_error_format)
                    EmailValidationResult.TOO_LONG -> stringResource(R.string.showcase_email_error_too_long)
                    EmailValidationResult.VALID, null -> null
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSubmitEmail() }),
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                AppButton(
                    text = stringResource(R.string.showcase_action_submit),
                    onClick = onSubmitEmail,
                    leadingIcon = AppIcons.Check,
                )
                AppButton(
                    text = stringResource(R.string.showcase_action_show_dialog),
                    onClick = { showDialog = true },
                    style = AppButtonStyle.SECONDARY,
                )
                AppButton(text = stringResource(R.string.showcase_action_disabled), onClick = {}, enabled = false)
            }
        }
    }
    if (showDialog) {
        AppDialog(
            title = stringResource(R.string.showcase_dialog_title),
            message = stringResource(R.string.showcase_dialog_message),
            confirmLabel = stringResource(R.string.action_ok),
            onConfirm = { showDialog = false },
            onDismiss = { showDialog = false },
            dismissLabel = stringResource(R.string.action_cancel),
        )
    }
}

@Composable
private fun StatesCard(selected: DemoContentState, onSelected: (DemoContentState) -> Unit) {
    val states = DemoContentState.entries
    AppCard(modifier = Modifier.fillMaxWidth()) {
        SectionTitle(stringResource(R.string.showcase_section_states))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            states.forEachIndexed { index, state ->
                SegmentedButton(
                    selected = state == selected,
                    onClick = { onSelected(state) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = states.size),
                    label = { Text(stringResource(state.labelRes), maxLines = 1) },
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = StatePanelMinHeight)
                .testTag(ShowcaseTestTags.STATE_PANEL),
            contentAlignment = Alignment.Center,
        ) {
            when (selected) {
                DemoContentState.LOADING -> AppLoadingIndicator(message = stringResource(R.string.showcase_state_loading_message))
                DemoContentState.EMPTY -> AppEmptyState(
                    title = stringResource(R.string.showcase_state_empty_title),
                    message = stringResource(R.string.showcase_state_empty_message),
                )
                DemoContentState.ERROR -> AppErrorView(
                    message = stringResource(R.string.showcase_state_error_message),
                    onRetry = { onSelected(DemoContentState.CONTENT) },
                )
                DemoContentState.CONTENT -> Text(
                    text = stringResource(R.string.showcase_state_content_message),
                    style = AppDesignSystem.typography.bodyLarge,
                )
            }
        }
    }
}

private val DemoContentState.labelRes: Int
    get() = when (this) {
        DemoContentState.CONTENT -> R.string.showcase_state_content
        DemoContentState.LOADING -> R.string.showcase_state_loading
        DemoContentState.EMPTY -> R.string.showcase_state_empty
        DemoContentState.ERROR -> R.string.showcase_state_error
    }

@ScreenPreviews
@Composable
private fun DesignShowcaseScreenPreview() {
    AppTheme {
        DesignShowcaseScreen(
            uiState = DesignShowcaseUiState(EnvironmentInfo("dev", "https://api-devgs.vplayed.com/", "1.0")),
            onEmailChange = {},
            onSubmitEmail = {},
            onDemoStateSelected = {},
        )
    }
}
