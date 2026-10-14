package com.vplayed.feature.splashscreen.presentation.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vplayed.R
import com.vplayed.core.designsystem.dimension.WindowWidthClass
import com.vplayed.core.designsystem.dimension.currentWindowWidthClass
import com.vplayed.core.designsystem.shape.AppMotion
import com.vplayed.core.designsystem.theme.AppDesignSystem
import com.vplayed.core.designsystem.theme.AppTheme
import com.vplayed.core.ui.preview.ScreenPreviews
import com.vplayed.feature.splashscreen.presentation.viewmodel.SplashUiState
import com.vplayed.feature.splashscreen.presentation.viewmodel.SplashViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** Width / height of the logo vector (viewport 500 × 95). */
private const val LOGO_ASPECT_RATIO = 500f / 95f
private const val LOGO_START_SCALE = 0.92f

object SplashTestTags {
    const val LOGO = "splash_logo"
}

@Composable
fun SplashRoute(
    onSplashFinished: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SplashScreen(isReady = uiState == SplashUiState.Ready, onSplashFinished = onSplashFinished)
}

/**
 * First screen after the system splash. Uses the same `splash_background` color as the system
 * splash and window, and resource-qualified logos (`drawable` / `drawable-night`), so it follows
 * the system light/dark setting exactly like the system splash — no color jump on launch.
 *
 * Leaves when both the logo entrance animation has finished and [isReady] is true.
 */
@Composable
fun SplashScreen(
    isReady: Boolean,
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = AppDesignSystem.dimens
    val logoWidth = if (currentWindowWidthClass() == WindowWidthClass.COMPACT) {
        dimens.splashLogoWidth
    } else {
        dimens.splashLogoWidthLarge
    }

    val alpha = remember { Animatable(0f) }
    val scale = remember { Animatable(LOGO_START_SCALE) }
    var entranceFinished by rememberSaveable { mutableStateOf(false) }
    val currentOnSplashFinished by rememberUpdatedState(onSplashFinished)

    LaunchedEffect(Unit) {
        if (!entranceFinished) {
            coroutineScope {
                launch { alpha.animateTo(1f, AppMotion.standard(AppMotion.LONG_MILLIS)) }
                launch { scale.animateTo(1f, AppMotion.standard(AppMotion.LONG_MILLIS)) }
            }
            entranceFinished = true
        } else {
            alpha.snapTo(1f)
            scale.snapTo(1f)
        }
    }
    LaunchedEffect(entranceFinished, isReady) {
        if (entranceFinished && isReady) currentOnSplashFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colorResource(R.color.splash_background)),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_vplayed_logo),
            contentDescription = stringResource(R.string.splash_logo_description),
            modifier = Modifier
                .width(logoWidth)
                .aspectRatio(LOGO_ASPECT_RATIO)
                .graphicsLayer {
                    this.alpha = alpha.value
                    scaleX = scale.value
                    scaleY = scale.value
                }
                .testTag(SplashTestTags.LOGO),
        )
    }
}

@ScreenPreviews
@Composable
private fun SplashScreenPreview() {
    AppTheme { SplashScreen(isReady = false, onSplashFinished = {}) }
}
