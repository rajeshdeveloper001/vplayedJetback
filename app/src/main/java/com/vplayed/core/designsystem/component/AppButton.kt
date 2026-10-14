package com.vplayed.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vplayed.R
import com.vplayed.core.designsystem.icon.AppIcons
import com.vplayed.core.designsystem.theme.AppDesignSystem
import com.vplayed.core.designsystem.theme.AppTheme
import com.vplayed.core.ui.preview.ComponentPreviews

enum class AppButtonStyle { PRIMARY, SECONDARY, TEXT }

/**
 * Standard button. Height, shape and padding come from the design system so every button in the
 * app changes together. While [loading], the button ignores clicks and announces a busy state.
 */
@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: AppButtonStyle = AppButtonStyle.PRIMARY,
    enabled: Boolean = true,
    loading: Boolean = false,
    @DrawableRes leadingIcon: Int? = null,
) {
    val dimens = AppDesignSystem.dimens
    val shape = AppDesignSystem.shapes.button
    val sizedModifier = modifier.heightIn(min = dimens.buttonHeight)
    val padding = PaddingValues(horizontal = dimens.spacing.xl, vertical = dimens.spacing.xs)
    val isEnabled = enabled && !loading
    val content: @Composable RowScope.() -> Unit = {
        ButtonContent(text = text, loading = loading, leadingIcon = leadingIcon)
    }
    when (style) {
        AppButtonStyle.PRIMARY -> Button(
            onClick = onClick,
            modifier = sizedModifier,
            enabled = isEnabled,
            shape = shape,
            contentPadding = padding,
            content = content,
        )
        AppButtonStyle.SECONDARY -> OutlinedButton(
            onClick = onClick,
            modifier = sizedModifier,
            enabled = isEnabled,
            shape = shape,
            contentPadding = padding,
            content = content,
        )
        AppButtonStyle.TEXT -> TextButton(
            onClick = onClick,
            modifier = sizedModifier,
            enabled = isEnabled,
            shape = shape,
            contentPadding = padding,
            content = content,
        )
    }
}

@Composable
private fun RowScope.ButtonContent(text: String, loading: Boolean, @DrawableRes leadingIcon: Int?) {
    val dimens = AppDesignSystem.dimens
    if (loading) {
        val loadingDescription = stringResource(R.string.cd_loading)
        CircularProgressIndicator(
            modifier = Modifier
                .size(dimens.icon.small)
                .semantics { contentDescription = loadingDescription },
            color = LocalContentColor.current,
            strokeWidth = 2.dp,
        )
        Spacer(Modifier.width(dimens.spacing.xs))
    } else if (leadingIcon != null) {
        Icon(
            painter = painterResource(leadingIcon),
            contentDescription = null,
            modifier = Modifier.size(dimens.icon.small),
        )
        Spacer(Modifier.width(dimens.spacing.xs))
    }
    Text(
        text = text,
        style = AppDesignSystem.typography.labelLarge,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.align(Alignment.CenterVertically),
    )
}

@ComponentPreviews
@Composable
private fun AppButtonPreview() {
    AppTheme {
        AppSurface {
            Column(
                verticalArrangement = Arrangement.spacedBy(AppDesignSystem.dimens.spacing.xs),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(AppDesignSystem.dimens.spacing.xs)) {
                    AppButton(text = "Primary", onClick = {}, leadingIcon = AppIcons.Check)
                    AppButton(text = "Secondary", onClick = {}, style = AppButtonStyle.SECONDARY)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(AppDesignSystem.dimens.spacing.xs)) {
                    AppButton(text = "Text", onClick = {}, style = AppButtonStyle.TEXT)
                    AppButton(text = "Saving", onClick = {}, loading = true)
                    AppButton(text = "Disabled", onClick = {}, enabled = false)
                }
            }
        }
    }
}
