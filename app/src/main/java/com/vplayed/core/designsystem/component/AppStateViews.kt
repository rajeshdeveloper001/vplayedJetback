package com.vplayed.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.vplayed.core.designsystem.icon.AppIcons
import com.vplayed.core.designsystem.theme.AppDesignSystem
import com.vplayed.core.designsystem.theme.AppTheme
import com.vplayed.core.ui.preview.ComponentPreviews
import com.vplayed.shared.l10n.Res
import com.vplayed.shared.l10n.common_error_title
import com.vplayed.shared.l10n.common_loading
import com.vplayed.shared.l10n.common_try_again
import org.jetbrains.compose.resources.stringResource

/** Centered progress indicator with an optional message. Announced to accessibility services. */
@Composable
fun AppLoadingIndicator(
    modifier: Modifier = Modifier,
    message: String? = null,
) {
    val description = message ?: stringResource(Res.string.common_loading)
    Column(
        modifier = modifier
            .padding(AppDesignSystem.dimens.spacing.xl)
            .semantics(mergeDescendants = true) {
                contentDescription = description
                liveRegion = LiveRegionMode.Polite
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppDesignSystem.dimens.spacing.md),
    ) {
        CircularProgressIndicator(color = AppDesignSystem.colors.primary)
        if (message != null) {
            Text(message, style = AppDesignSystem.typography.bodyMedium, textAlign = TextAlign.Center)
        }
    }
}

/** Full-area error state with an optional retry action. */
@Composable
fun AppErrorView(
    message: String,
    modifier: Modifier = Modifier,
    title: String = stringResource(Res.string.common_error_title),
    onRetry: (() -> Unit)? = null,
) {
    MessageState(
        icon = AppIcons.Error,
        iconTint = AppDesignSystem.colors.error,
        title = title,
        message = message,
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Assertive },
        actionLabel = onRetry?.let { stringResource(Res.string.common_try_again) },
        onAction = onRetry,
    )
}

/** Full-area empty state with an optional call to action. */
@Composable
fun AppEmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int = AppIcons.Empty,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    MessageState(
        icon = icon,
        iconTint = AppDesignSystem.colors.onSurfaceVariant,
        title = title,
        message = message,
        modifier = modifier,
        actionLabel = actionLabel,
        onAction = onAction,
    )
}

@Composable
private fun MessageState(
    @DrawableRes icon: Int,
    iconTint: Color,
    title: String,
    message: String,
    modifier: Modifier,
    actionLabel: String?,
    onAction: (() -> Unit)?,
) {
    val dimens = AppDesignSystem.dimens
    Column(
        modifier = modifier
            .widthIn(max = dimens.readableContentMaxWidth)
            .fillMaxWidth()
            .padding(dimens.spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimens.spacing.sm),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(dimens.icon.illustration),
        )
        Text(title, style = AppDesignSystem.typography.titleLarge, textAlign = TextAlign.Center)
        Text(
            message,
            style = AppDesignSystem.typography.bodyMedium,
            color = AppDesignSystem.colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            AppButton(text = actionLabel, onClick = onAction, style = AppButtonStyle.SECONDARY)
        }
    }
}

/** Standard confirmation dialog. Omit [dismissLabel] for an acknowledgement-only dialog. */
@Composable
fun AppDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissLabel: String? = null,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { AppButton(text = confirmLabel, onClick = onConfirm, style = AppButtonStyle.TEXT) },
        dismissButton = dismissLabel?.let {
            { AppButton(text = it, onClick = onDismiss, style = AppButtonStyle.TEXT) }
        },
        shape = AppDesignSystem.shapes.dialog,
        containerColor = AppDesignSystem.colors.surfaceContainerHigh,
        tonalElevation = AppDesignSystem.elevations.dialog,
    )
}

@ComponentPreviews
@Composable
private fun StateViewsPreview() {
    AppTheme {
        AppSurface {
            Column {
                AppLoadingIndicator(message = "Loading content")
                AppEmptyState(title = "Nothing here yet", message = "Content you save will appear here.")
                AppErrorView(message = "Check your connection and try again.", onRetry = {})
            }
        }
    }
}
