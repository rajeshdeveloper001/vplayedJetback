package com.vplayed.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import com.vplayed.core.designsystem.icon.AppIcons
import com.vplayed.core.designsystem.theme.AppDesignSystem
import com.vplayed.core.designsystem.theme.AppTheme
import com.vplayed.core.ui.preview.ComponentPreviews
import com.vplayed.shared.l10n.Res
import com.vplayed.shared.l10n.common_back
import org.jetbrains.compose.resources.stringResource

/** Themed background container. Use as the root of screens and previews. */
@Composable
fun AppSurface(
    modifier: Modifier = Modifier,
    color: Color = AppDesignSystem.colors.background,
    content: @Composable () -> Unit,
) {
    Surface(modifier = modifier, color = color, content = content)
}

/**
 * Standard card with design-system shape, elevation and inner padding. Pass [onClick] to make the
 * whole card a single accessible click target.
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = AppDesignSystem.shapes.card
    val colors = CardDefaults.cardColors(containerColor = AppDesignSystem.colors.surfaceContainerLow)
    val elevation = CardDefaults.cardElevation(defaultElevation = AppDesignSystem.elevations.card)
    val inner: @Composable ColumnScope.() -> Unit = {
        Column(modifier = Modifier.padding(AppDesignSystem.dimens.cardContentPadding), content = content)
    }
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier, shape = shape, colors = colors, elevation = elevation, content = inner)
    } else {
        Card(modifier = modifier, shape = shape, colors = colors, elevation = elevation, content = inner)
    }
}

/**
 * Standard top app bar. Shows a back button when [onNavigateBack] is set. Applies status-bar
 * insets itself, so it works edge-to-edge in every orientation.
 */
@OptIn(ExperimentalMaterial3Api::class) // TopAppBar is still experimental in Material3 1.4.
@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    TopAppBar(
        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        modifier = modifier,
        navigationIcon = {
            if (onNavigateBack != null) {
                IconButton(onClick = onNavigateBack) {
                    Icon(painterResource(AppIcons.Back), contentDescription = stringResource(Res.string.common_back))
                }
            }
        },
        expandedHeight = AppDesignSystem.dimens.toolbarHeight,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = AppDesignSystem.colors.surface,
            scrolledContainerColor = AppDesignSystem.colors.surfaceContainer,
        ),
        scrollBehavior = scrollBehavior,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@ComponentPreviews
@Composable
private fun AppCardPreview() {
    AppTheme {
        AppSurface {
            Column {
                AppTopBar(title = "Title", onNavigateBack = {})
                AppCard(modifier = Modifier.padding(AppDesignSystem.dimens.spacing.md)) {
                    Text("Card title", style = AppDesignSystem.typography.titleMedium)
                    Text("Supporting text inside a card.", style = AppDesignSystem.typography.bodyMedium)
                }
            }
        }
    }
}
