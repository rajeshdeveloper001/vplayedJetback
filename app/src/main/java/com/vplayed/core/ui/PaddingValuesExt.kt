package com.vplayed.core.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp

/** Adds screen padding on top of inset padding (e.g. from [AppScreenScaffold]), respecting RTL. */
@Composable
fun PaddingValues.plus(horizontal: Dp, vertical: Dp): PaddingValues {
    val direction = LocalLayoutDirection.current
    return PaddingValues(
        start = calculateStartPadding(direction) + horizontal,
        end = calculateEndPadding(direction) + horizontal,
        top = calculateTopPadding() + vertical,
        bottom = calculateBottomPadding() + vertical,
    )
}
