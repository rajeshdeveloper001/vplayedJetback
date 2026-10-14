package com.vplayed.core.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.vplayed.core.designsystem.theme.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * Shared Fragment behaviour for Compose screens hosted as Fragments.
 *
 * - The view is a [ComposeView] disposed with the view lifecycle (no leaked compositions).
 * - [ScreenContent] is wrapped in [AppTheme] using the host activity's [AppearanceViewModel], so the
 *   screen is themed the same whether or not it sits inside a parent composition.
 * - [collectOnViewLifecycle] collects only while the view is at least STARTED.
 *
 * Subclasses never keep references to the view.
 */
abstract class BaseFragment : Fragment() {

    private val appearanceViewModel: AppearanceViewModel by activityViewModels()

    /** The screen. (Not named `Content`: inside the [ComposeView] that would call `ComposeView.Content`.) */
    @Composable
    protected abstract fun ScreenContent()

    final override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(inflater.context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            val uiState by appearanceViewModel.uiState.collectAsStateWithLifecycle()
            val appearance = (uiState as? AppearanceUiState.Ready)?.appearance ?: return@setContent
            AppTheme(appearance = appearance) { ScreenContent() }
        }
    }

    /** Collects [this] flow on the view lifecycle; stops when the view is stopped or destroyed. */
    protected fun <T> Flow<T>.collectOnViewLifecycle(action: suspend (T) -> Unit) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                collect { action(it) }
            }
        }
    }

    /** Shows a short Toast; ignored once the fragment is detached. */
    protected fun showToast(message: String) {
        val context = context ?: return
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
