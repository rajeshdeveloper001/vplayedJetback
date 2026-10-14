package com.vplayed.app

import android.os.Bundle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.vplayed.core.ui.base.AppearanceUiState
import com.vplayed.core.ui.base.BaseActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must run before super.onCreate. The system splash (background only) stays until the
        // appearance preferences are read, so the first themed frame is never wrong.
        installSplashScreen().setKeepOnScreenCondition { appearanceViewModel.uiState.value is AppearanceUiState.Loading }
        super.onCreate(savedInstanceState)
        setThemedContent { VPlayedApp() }
    }
}
