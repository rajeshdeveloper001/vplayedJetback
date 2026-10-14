package com.vplayed.feature.dashboard.home.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vplayed.shared.home.HomeFeedController
import com.vplayed.shared.home.HomeFeedState
import com.vplayed.shared.home.HomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

/** The Home screen state (the shared feed state; `PagingStatus` is shared too). */
typealias HomeUiState = HomeFeedState

/**
 * Home page logic, shared with VPlayed since Phase 4: [HomeFeedController] (../kmp) loads banners and
 * rows, pages rows as the user scrolls ([loadNextPage]) and items within a row ([loadMoreItems]),
 * drops empty and duplicate rows and keeps the content when a refresh fails. This ViewModel runs it
 * on [viewModelScope] and starts the first load.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(repository: HomeRepository) : ViewModel() {

    private val controller = HomeFeedController(repository, viewModelScope)

    val uiState: StateFlow<HomeUiState> = controller.state

    init {
        controller.load()
    }

    /** Retry after a full-screen error. */
    fun retry() = controller.retry()

    /** Pull-to-refresh: reloads banners and page 1, keeping current content until it succeeds. */
    fun refresh() = controller.refresh()

    /** Loads the next page of rows. Safe to call repeatedly; it ignores calls while busy or at the end. */
    fun loadNextPage() = controller.loadNextPage()

    /** Loads the next items of one row. Ignored when the row is complete or already loading. */
    fun loadMoreItems(sectionId: String) = controller.loadMoreItems(sectionId)
}
