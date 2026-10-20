package com.vplayed.feature.dashboard.subscription.presentation.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.vplayed.core.designsystem.component.AppEmptyState
import com.vplayed.core.designsystem.icon.AppIcons
import com.vplayed.core.designsystem.theme.AppTheme
import com.vplayed.core.ui.AppScreenScaffold
import com.vplayed.core.ui.preview.ScreenPreviews
import com.vplayed.shared.l10n.Res
import com.vplayed.shared.l10n.nav_subscription
import com.vplayed.shared.l10n.subscription_coming_soon_message
import com.vplayed.shared.l10n.subscription_coming_soon_title
import org.jetbrains.compose.resources.stringResource

/** Subscription tab. Placeholder until its API and design are provided. */
@Composable
fun SubscriptionScreen(modifier: Modifier = Modifier) {
    AppScreenScaffold(title = stringResource(Res.string.nav_subscription), modifier = modifier) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            AppEmptyState(
                title = stringResource(Res.string.subscription_coming_soon_title),
                message = stringResource(Res.string.subscription_coming_soon_message),
                icon = AppIcons.SubscriptionOutlined,
            )
        }
    }
}

@ScreenPreviews
@Composable
private fun SubscriptionScreenPreview() {
    AppTheme { SubscriptionScreen() }
}
