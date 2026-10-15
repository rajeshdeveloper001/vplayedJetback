package com.vplayed.feature.dashboard.home

import com.vplayed.feature.dashboard.home.presentation.viewmodel.HomeViewModel
import com.vplayed.shared.core.error.AppError
import com.vplayed.shared.core.error.DataResult
import com.vplayed.shared.home.PagingStatus
import com.vplayed.shared.home.SectionItemsPage
import com.vplayed.testing.FakeHomeRepository
import com.vplayed.testing.FakeHomeRepository.Companion.item
import com.vplayed.testing.FakeHomeRepository.Companion.page
import com.vplayed.testing.FakeHomeRepository.Companion.section
import com.vplayed.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * The ViewModel runs the shared `HomeFeedController` (../kmp), whose rules are tested there
 * (`HomeFeedControllerTest`). These tests check the wiring: the first load starts on creation and
 * each action reaches the controller.
 */
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeHomeRepository().apply {
        sectionPages[1] = page(section("a"), section("b", nextItemsPage = 2), next = 2)
        sectionPages[2] = page(section("c"), next = null)
    }

    private fun viewModel() = HomeViewModel(repository)

    private val HomeViewModel.state get() = uiState.value

    @Test
    fun givenApiSucceeds_whenOpened_thenShowsBannersAndFirstPageRows() {
        val vm = viewModel()

        assertFalse(vm.state.isLoading)
        assertEquals(listOf("b1"), vm.state.banners.map { it.id })
        assertEquals(listOf("a", "b"), vm.state.sections.map { it.id })
        assertEquals(PagingStatus.Idle, vm.state.paging)
    }

    @Test
    fun givenFirstPageInFlight_whenOpened_thenShowsLoadingShimmerState() {
        val hold = repository.holdSections(1)
        val vm = viewModel()

        assertTrue(vm.state.isLoading)

        hold.complete(Unit)
        assertFalse(vm.state.isLoading)
    }

    @Test
    fun givenUserScrollsToEnd_whenLoadingNextPage_thenRowsAppendUntilEndReached() {
        val vm = viewModel()

        vm.loadNextPage()

        assertEquals(listOf("a", "b", "c"), vm.state.sections.map { it.id })
        assertEquals(PagingStatus.EndReached, vm.state.paging)
    }

    @Test
    fun givenRowWithMoreItems_whenUserScrollsRow_thenItemsAppend() {
        repository.itemPages["b" to 2] = DataResult.Success(SectionItemsPage(listOf(item("b-new")), nextPage = null))
        val vm = viewModel()

        vm.loadMoreItems("b")

        assertEquals(listOf("b-0", "b-1", "b-2", "b-new"), vm.state.sections.first { it.id == "b" }.items.map { it.id })
    }

    @Test
    fun givenEverythingFails_whenOpened_thenShowsFullScreenErrorAndRetryRecovers() {
        repository.banners = DataResult.Failure(AppError.Network)
        repository.sectionPages[1] = DataResult.Failure(AppError.Network)
        val vm = viewModel()

        assertEquals(AppError.Network, vm.state.error)

        repository.sectionPages[1] = page(section("a"), next = null)
        vm.retry()

        assertNull(vm.state.error)
        assertEquals(listOf("a"), vm.state.sections.map { it.id })
    }

    @Test
    fun givenContentShown_whenRefreshSucceeds_thenReplacesRows() {
        val vm = viewModel()
        repository.sectionPages[1] = page(section("z"), next = null)

        vm.refresh()

        assertFalse(vm.state.isRefreshing)
        assertEquals(listOf("z"), vm.state.sections.map { it.id })
    }
}
