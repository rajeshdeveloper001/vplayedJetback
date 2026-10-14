package com.vplayed.feature.auth.presentation

import android.os.Bundle
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vplayed.core.ui.base.BaseFragment
import com.vplayed.feature.auth.presentation.screen.LoginActions
import com.vplayed.feature.auth.presentation.screen.LoginScreen
import com.vplayed.feature.auth.presentation.viewmodel.LoginEvent
import com.vplayed.feature.auth.presentation.viewmodel.LoginViewModel
import com.vplayed.feature.auth.presentation.viewmodel.resolve
import dagger.hilt.android.AndroidEntryPoint

/**
 * Login, email step. A Fragment hosting the Compose [LoginScreen]; it is placed in the Compose
 * NavHost with `AndroidFragment` (see `LoginNavigation`). When the backend sends a code the host
 * opens the OTP screen; every other answer is a Toast.
 */
@AndroidEntryPoint
class LoginFragment : BaseFragment() {

    // Activity-scoped: the typed email survives the OTP screen (this fragment is removed meanwhile).
    private val viewModel: LoginViewModel by activityViewModels()

    /** Set by the host on every composition; invoked by the close button and "Continue as guest". */
    var onCloseRequested: (() -> Unit)? = null

    /** Set by the host: open the OTP screen for the email (and on the device step when the limit is reached). */
    var onOpenOtp: ((email: String, startWithDeviceLimit: Boolean) -> Unit)? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.events.collectOnViewLifecycle { event ->
            when (event) {
                is LoginEvent.ShowMessage -> showToast(event.message.resolve(requireContext()))
                is LoginEvent.OpenOtp -> onOpenOtp?.invoke(event.email, event.startWithDeviceLimit)
            }
        }
    }

    @Composable
    override fun ScreenContent() {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val actions = remember(viewModel) {
            LoginActions(
                onEmailChange = viewModel::onEmailChange,
                onEmailFocusLost = viewModel::onEmailFocusLost,
                onSubmit = viewModel::onSubmit,
                onClose = {
                    viewModel.onClosed()
                    onCloseRequested?.invoke()
                },
            )
        }
        LoginScreen(uiState = uiState, actions = actions)
    }

    override fun onDestroy() {
        onCloseRequested = null
        onOpenOtp = null
        super.onDestroy()
    }
}
