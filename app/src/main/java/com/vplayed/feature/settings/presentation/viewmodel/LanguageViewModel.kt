package com.vplayed.feature.settings.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vplayed.shared.SharedGraph
import com.vplayed.shared.language.Language
import com.vplayed.shared.language.SupportedLanguagesStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * The languages Menu → Language offers: the backend's list (`mediaview/api/v1/language`, loaded at
 * launch into the shared [SupportedLanguagesStore]), else the languages the app has translations for.
 */
@HiltViewModel
class LanguageViewModel @Inject constructor(
    private val graph: SharedGraph,
) : ViewModel() {

    val languages: StateFlow<List<Language>> = graph.supportedLanguagesStore.languages

    init {
        // The launch-time load may have failed (offline start): try again when the screen opens.
        if (languages.value == Language.FALLBACK) {
            viewModelScope.launch { graph.languageRepository.load() }
        }
    }

    /** The language shown now: the user's choice or the best match for [preferred] tags, else the backend's default. */
    fun currentLanguage(preferred: List<String>): Language = graph.supportedLanguagesStore.resolve(preferred)
}
