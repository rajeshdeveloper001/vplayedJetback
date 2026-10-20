package com.vplayed.feature.auth.presentation

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vplayed.core.ui.base.BaseFragment
import com.vplayed.feature.auth.presentation.screen.OtpActions
import com.vplayed.feature.auth.presentation.screen.OtpScreen
import com.vplayed.feature.auth.presentation.screen.RegistrationActions
import com.vplayed.feature.auth.presentation.viewmodel.OtpEvent
import com.vplayed.feature.auth.presentation.viewmodel.OtpViewModel
import com.vplayed.feature.auth.presentation.viewmodel.resolve
import dagger.hilt.android.AndroidEntryPoint

/**
 * Email login, verification steps (code, device limit, sign-up of a new account, success). A Fragment hosting the Compose
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
                is OtpEvent.LeftSignUp -> {
                    Toast.makeText(requireContext(), event.message.resolve(requireContext()), Toast.LENGTH_LONG).show()
                    onBackRequested?.invoke()
                }
            }
        }
    }

    @Composable
    override fun ScreenContent() {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val registration by viewModel.registrationState.collectAsStateWithLifecycle()
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
        val registrationActions = remember(viewModel) {
            RegistrationActions(
                onNameChange = viewModel::onNameChange,
                onEmailChange = viewModel::onRegistrationEmailChange,
                onCountryChange = viewModel::onRegistrationCountryChange,
                onNumberChange = viewModel::onRegistrationNumberChange,
                onShowReferral = viewModel::onShowReferral,
                onReferralChange = viewModel::onReferralChange,
                onSubmit = viewModel::onSubmitRegistration,
                onBack = viewModel::onRegistrationBack,
            )
        }
        OtpScreen(uiState = uiState, actions = actions, registration = registration, registrationActions = registrationActions)
    }

    override fun onDestroy() {
        onBackRequested = null
        onCloseRequested = null
        onAuthenticated = null
        super.onDestroy()
    }
}
