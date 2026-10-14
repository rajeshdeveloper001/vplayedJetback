package com.vplayed.feature.dashboard.home.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vplayed.core.error.AppError
import com.vplayed.core.error.DataResult
import com.vplayed.feature.dashboard.home.domain.HomeRepository
import com.vplayed.feature.dashboard.home.domain.model.ContentItem
import com.vplayed.feature.dashboard.home.domain.model.HomeBanner
import com.vplayed.feature.dashboard.home.domain.model.HomeSection
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** State of loading further home rows (vertical pagination). */
sealed interface PagingStatus {
    /** More pages may exist; the next one loads when the user nears the end. */
    data object Idle : PagingStatus
    data object Loading : PagingStatus
    data object EndReached : PagingStatus
    data class Failed(val error: AppError) : PagingStatus
}

data class HomeUiState(
    /** First load in progress: the full-screen shimmer is shown. */
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val banners: List<HomeBanner> = emptyList(),
    val sections: List<HomeSection> = emptyList(),
    val paging: PagingStatus = PagingStatus.Idle,
    /** Next page of rows to request; null when every page is loaded. */
    val nextPage: Int? = 1,
    /** Rows currently fetching more items (horizontal pagination). */
    val loadingSectionIds: Set<String> = emptySet(),
    /** Set when nothing could be loaded at all; the full-screen error with Retry is shown. */
    val error: AppError? = null,
) {
    val isEmpty: Boolean
        get() = !isLoading && error == null && banners.isEmpty() && sections.isEmpty() && paging == PagingStatus.EndReached
}

/**
 * Home page logic (replaces the previous app's ShocialFragment/ShocialViewModel):
 * - banners and the first page of rows load together; a banner failure alone is not fatal;
 * - further pages load as the user scrolls ([loadNextPage]) instead of chaining every page up front;
 * - rows without items are dropped and duplicate rows (same id) are ignored;
 * - each row pages its own items when scrolled horizontally ([loadMoreItems]).
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: HomeRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadFirstPage(isRefresh = false)
    }

    /** Retry after a full-screen error. */
    fun retry() {
        _uiState.value = HomeUiState()
        loadFirstPage(isRefresh = false)
    }

    /** Pull-to-refresh: reloads banners and page 1, keeping current content until it succeeds. */
    fun refresh() {
        if (_uiState.value.isRefreshing || _uiState.value.isLoading) return
        _uiState.update { it.copy(isRefreshing = true) }
        loadFirstPage(isRefresh = true)
    }

    /** Loads the next page of rows. Safe to call repeatedly; it ignores calls while busy or at the end. */
    fun loadNextPage() {
        val state = _uiState.value
        val page = state.nextPage ?: return
        if (state.isLoading || state.isRefreshing || state.paging == PagingStatus.Loading) return
        _uiState.update { it.copy(paging = PagingStatus.Loading) }
        viewModelScope.launch {
            when (val result = repository.getSections(page)) {
                is DataResult.Success -> _uiState.update { current ->
                    current.copy(
                        sections = current.sections.appendDistinct(result.data.sections),
                        nextPage = result.data.nextPage,
                        paging = if (result.data.nextPage == null) PagingStatus.EndReached else PagingStatus.Idle,
                    )
                }
                is DataResult.Failure -> _uiState.update { it.copy(paging = PagingStatus.Failed(result.error)) }
            }
        }
    }

    /** Loads the next items of one row. Ignored when the row is complete or already loading. */
    fun loadMoreItems(sectionId: String) {
        val state = _uiState.value
        val section = state.sections.firstOrNull { it.id == sectionId } ?: return
        val page = section.nextItemsPage ?: return
        if (sectionId in state.loadingSectionIds) return
        _uiState.update { it.copy(loadingSectionIds = it.loadingSectionIds + sectionId) }
        viewModelScope.launch {
            val result = repository.getSectionItems(sectionId, page)
            _uiState.update { current ->
                val sections = if (result is DataResult.Success) {
                    current.sections.map { row ->
                        if (row.id != sectionId) row
                        else row.copy(
                            items = row.items.appendDistinctItems(result.data.items),
                            nextItemsPage = result.data.nextPage,
                        )
                    }
                } else {
                    // Keep the row as is; scrolling to its end again retries.
                    current.sections
                }
                current.copy(sections = sections, loadingSectionIds = current.loadingSectionIds - sectionId)
            }
        }
    }

    private fun loadFirstPage(isRefresh: Boolean) {
        viewModelScope.launch {
            val banners = async { repository.getBanners() }
            val firstPage = repository.getSections(page = 1)
            val bannerResult = banners.await()
            _uiState.update { current ->
                val loadedBanners = (bannerResult as? DataResult.Success)?.data
                when (firstPage) {
                    is DataResult.Success -> HomeUiState(
                        isLoading = false,
                        banners = loadedBanners ?: current.banners.takeIf { isRefresh }.orEmpty(),
                        sections = emptyList<HomeSection>().appendDistinct(firstPage.data.sections),
                        nextPage = firstPage.data.nextPage,
                        paging = if (firstPage.data.nextPage == null) PagingStatus.EndReached else PagingStatus.Idle,
                    )
                    is DataResult.Failure -> when {
                        // Refresh failed: keep what is on screen.
                        isRefresh -> current.copy(isRefreshing = false)
                        // Banners loaded: show them; the rows area offers a retry.
                        !loadedBanners.isNullOrEmpty() -> current.copy(
                            isLoading = false,
                            banners = loadedBanners,
                            paging = PagingStatus.Failed(firstPage.error),
                        )
                        else -> current.copy(isLoading = false, error = firstPage.error)
                    }
                }
            }
        }
    }

    private companion object {
        fun List<HomeSection>.appendDistinct(more: List<HomeSection>): List<HomeSection> {
            val known = mapTo(HashSet()) { it.id }
            return this + more.filter { known.add(it.id) }
        }

        fun List<ContentItem>.appendDistinctItems(more: List<ContentItem>): List<ContentItem> {
            val known = mapTo(HashSet()) { it.id }
            return this + more.filter { known.add(it.id) }
        }
    }
}
