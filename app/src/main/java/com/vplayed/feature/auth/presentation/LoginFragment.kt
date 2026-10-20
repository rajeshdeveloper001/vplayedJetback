package com.vplayed.feature.auth.presentation

import android.os.Bundle
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.vplayed.R
import com.vplayed.core.ui.base.BaseFragment
import com.vplayed.feature.auth.data.GoogleSignInClient
import com.vplayed.feature.auth.data.GoogleSignInOutcome
import com.vplayed.feature.auth.presentation.screen.LoginActions
import com.vplayed.feature.auth.presentation.screen.LoginScreen
import com.vplayed.feature.auth.presentation.viewmodel.LoginEvent
import com.vplayed.feature.auth.presentation.viewmodel.LoginViewModel
import com.vplayed.feature.auth.presentation.viewmodel.resolve
import com.vplayed.shared.auth.OtpChannelKind
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

/**
 * Login, step 1 (email or mobile number, Google, guest). A Fragment hosting the Compose
 * [LoginScreen]; it is placed in the Compose NavHost with `AndroidFragment` (see `LoginNavigation`).
 * When a code was requested the host opens the OTP screen; a started session closes Login; every
 * other answer is a Toast. The Google sheet runs here, on the fragment's lifecycle.
 */
@AndroidEntryPoint
class LoginFragment : BaseFragment() {

    // Activity-scoped: the typed email survives the OTP screen (this fragment is removed meanwhile).
    private val viewModel: LoginViewModel by activityViewModels()

    @Inject
    lateinit var googleSignIn: GoogleSignInClient

    /** Set by the host on every composition; invoked by the close button and when a session starts. */
    var onCloseRequested: (() -> Unit)? = null

    /**
     * Set by the host: open the OTP screen for the email or phone number (and on the device step when
     * the limit is reached; Google sign-in opens only the device step).
     */
    var onOpenOtp: ((destination: String, startWithDeviceLimit: Boolean, channel: OtpChannelKind, dialCode: String?) -> Unit)? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.events.collectOnViewLifecycle { event ->
            when (event) {
                is LoginEvent.ShowMessage -> showToast(event.message.resolve(requireContext()))
                is LoginEvent.OpenOtp -> onOpenOtp?.invoke(event.email, event.startWithDeviceLimit, event.channel, event.dialCode)
                is LoginEvent.Close -> {
                    if (event.showSignedIn) showToast(getString(R.string.login_signed_in))
                    viewModel.onClosed()
                    onCloseRequested?.invoke()
                }
            }
        }
    }

    @Composable
    override fun ScreenContent() {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val options by viewModel.loginOptions.collectAsStateWithLifecycle()
        val actions = remember(viewModel) {
            LoginActions(
                onEmailChange = viewModel::onEmailChange,
                onEmailFocusLost = viewModel::onEmailFocusLost,
                onPhoneChange = viewModel::onPhoneChange,
                onPhoneFocusLost = viewModel::onPhoneFocusLost,
                onCountryChange = viewModel::onCountryChange,
                onSubmit = viewModel::onSubmit,
                onGoogle = ::signInWithGoogle,
                onGuest = viewModel::onGuest,
                onClose = {
                    viewModel.onClosed()
                    onCloseRequested?.invoke()
                },
            )
        }
        LoginScreen(uiState = uiState, actions = actions, options = options)
    }

    private fun signInWithGoogle() {
        if (!viewModel.onGoogleStarted()) return
        val activity = requireActivity()
        viewLifecycleOwner.lifecycleScope.launch {
            // Reported even when the screen goes away mid-sheet, so the (activity-scoped) state is released.
            var outcome: GoogleSignInOutcome = GoogleSignInOutcome.Cancelled
            try {
                outcome = googleSignIn.signIn(activity)
            } finally {
                viewModel.onGoogleResult(outcome)
            }
        }
    }

    override fun onDestroy() {
        onCloseRequested = null
        onOpenOtp = null
        super.onDestroy()
    }
}
