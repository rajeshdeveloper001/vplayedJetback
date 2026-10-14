package com.vplayed.feature.dashboard.promos.presentation.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.vplayed.R
import com.vplayed.core.designsystem.component.AppEmptyState
import com.vplayed.core.designsystem.icon.AppIcons
import com.vplayed.core.designsystem.theme.AppTheme
import com.vplayed.core.ui.AppScreenScaffold
import com.vplayed.core.ui.preview.ScreenPreviews

/** Promos tab. Placeholder until its API and design are provided. */
@Composable
fun PromosScreen(modifier: Modifier = Modifier) {
    AppScreenScaffold(title = stringResource(R.string.nav_promos), modifier = modifier) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            AppEmptyState(
                title = stringResource(R.string.promos_coming_soon_title),
                message = stringResource(R.string.promos_coming_soon_message),
                icon = AppIcons.PromosOutlined,
            )
        }
    }
}

@ScreenPreviews
@Composable
private fun PromosScreenPreview() {
    AppTheme { PromosScreen() }
}
