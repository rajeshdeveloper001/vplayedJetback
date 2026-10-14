package com.vplayed.feature.auth.presentation

import android.os.Bundle
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vplayed.core.ui.base.BaseFragment
import com.vplayed.feature.auth.presentation.screen.OtpActions
import com.vplayed.feature.auth.presentation.screen.OtpScreen
import com.vplayed.feature.auth.presentation.viewmodel.OtpEvent
import com.vplayed.feature.auth.presentation.viewmodel.OtpViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * Email login, verification steps (code, device limit, success). A Fragment hosting the Compose
 * [OtpScreen]; it is placed in the Compose NavHost with `AndroidFragment` (see `OtpNavigation`) and
 * receives the email as a fragment argument. The host decides where Back, Close and a successful
 * sign-in lead.
 */
@AndroidEntryPoint
class OtpFragment : BaseFragment() {

    private val viewModel: OtpViewModel by viewModels()

    /** Set by the host: leave for the email step. */
    var onBackRequested: (() -> Unit)? = null

    /** Set by the host: the close button; leave sign-in altogether. */
    var onCloseRequested: (() -> Unit)? = null

    /** Set by the host: signed in and the success step is done. */
    var onAuthenticated: (() -> Unit)? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.events.collectOnViewLifecycle { event ->
            when (event) {
                OtpEvent.NavigateBack -> onBackRequested?.invoke()
                OtpEvent.Authenticated -> onAuthenticated?.invoke()
            }
        }
    }

    @Composable
    override fun ScreenContent() {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val actions = remember(viewModel) {
            OtpActions(
                onCodeChange = viewModel::onCodeChange,
                onVerify = viewModel::onVerify,
                onResend = viewModel::onResend,
                onBack = viewModel::onBack,
                onSignOutDevice = viewModel::onSignOutDevice,
                onSignOutAllDevices = viewModel::onSignOutAllDevices,
                onContinueAfterSignOut = viewModel::onContinueAfterSignOut,
                onClose = { onCloseRequested?.invoke() },
            )
        }
        OtpScreen(uiState = uiState, actions = actions)
    }

    override fun onDestroy() {
        onBackRequested = null
        onCloseRequested = null
        onAuthenticated = null
        super.onDestroy()
    }
}
