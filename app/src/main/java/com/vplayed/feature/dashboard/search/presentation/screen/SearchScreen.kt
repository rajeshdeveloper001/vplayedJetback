package com.vplayed.feature.dashboard.search.presentation.screen

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
import com.vplayed.shared.l10n.nav_search
import com.vplayed.shared.l10n.search_coming_soon_message
import com.vplayed.shared.l10n.search_coming_soon_title
import org.jetbrains.compose.resources.stringResource

/** Search tab. Placeholder until its API and design are provided. */
@Composable
fun SearchScreen(modifier: Modifier = Modifier) {
    AppScreenScaffold(title = stringResource(Res.string.nav_search), modifier = modifier) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            AppEmptyState(
                title = stringResource(Res.string.search_coming_soon_title),
                message = stringResource(Res.string.search_coming_soon_message),
                icon = AppIcons.Search,
            )
        }
    }
}

@ScreenPreviews
@Composable
private fun SearchScreenPreview() {
    AppTheme { SearchScreen() }
}
