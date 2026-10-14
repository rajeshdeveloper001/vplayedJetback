package com.vplayed.core.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.vplayed.core.designsystem.component.AppTopBar
import com.vplayed.core.designsystem.theme.AppDesignSystem

/**
 * Standard screen frame: themed background, top bar, and padding for every inset that can
 * overlap content in edge-to-edge mode (status/navigation bars, display cutouts in landscape,
 * and the on-screen keyboard). Screens must apply the provided [PaddingValues] to their content.
 */
@OptIn(ExperimentalMaterial3Api::class) // TopAppBar scroll behavior is experimental in Material3 1.4.
@Composable
fun AppScreenScaffold(
    title: String,
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { AppTopBar(title = title, onNavigateBack = onNavigateBack, scrollBehavior = scrollBehavior) },
        containerColor = AppDesignSystem.colors.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        content = content,
    )
}
