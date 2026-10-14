package com.vplayed.feature.dashboard.home

import com.vplayed.core.error.AppError
import com.vplayed.core.error.DataResult
import com.vplayed.feature.dashboard.home.domain.model.SectionItemsPage
import com.vplayed.feature.dashboard.home.presentation.viewmodel.HomeViewModel
import com.vplayed.feature.dashboard.home.presentation.viewmodel.PagingStatus
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

class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeHomeRepository().apply {
        sectionPages[1] = page(section("a"), section("b", nextItemsPage = 2), next = 2)
        sectionPages[2] = page(section("c"), next = 3)
        sectionPages[3] = page(section("d"), next = null)
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
    fun givenUserScrollsToEnd_whenLoadingNextPages_thenRowsAppendUntilEndReached() {
        val vm = viewModel()

        vm.loadNextPage()
        vm.loadNextPage()

        assertEquals(listOf("a", "b", "c", "d"), vm.state.sections.map { it.id })
        assertEquals(PagingStatus.EndReached, vm.state.paging)
        assertNull(vm.state.nextPage)

        vm.loadNextPage()
        assertEquals("No request after the last page", listOf(1, 2, 3), repository.sectionCalls)
    }

    @Test
    fun givenPageAlreadyLoading_whenScrollTriggersAgain_thenOnlyOneRequestIsMade() {
        val vm = viewModel()
        val hold = repository.holdSections(2)

        vm.loadNextPage()
        vm.loadNextPage()
        assertEquals(PagingStatus.Loading, vm.state.paging)

        hold.complete(Unit)
        assertEquals(listOf(1, 2), repository.sectionCalls)
    }

    @Test
    fun givenDuplicateRowFromServer_whenAppending_thenItIsShownOnce() {
        repository.sectionPages[2] = page(section("b"), section("c"), next = null)
        val vm = viewModel()

        vm.loadNextPage()

        assertEquals(listOf("a", "b", "c"), vm.state.sections.map { it.id })
    }

    @Test
    fun givenNextPageFails_whenUserTapsRetry_thenPageLoads() {
        repository.sectionPages[2] = DataResult.Failure(AppError.Network)
        val vm = viewModel()

        vm.loadNextPage()
        assertEquals(PagingStatus.Failed(AppError.Network), vm.state.paging)
        assertEquals(listOf("a", "b"), vm.state.sections.map { it.id })

        repository.sectionPages[2] = page(section("c"), next = null)
        vm.loadNextPage()
        assertEquals(listOf("a", "b", "c"), vm.state.sections.map { it.id })
    }

    @Test
    fun givenEverythingFails_whenOpened_thenShowsFullScreenErrorAndRetryRecovers() {
        repository.banners = DataResult.Failure(AppError.Network)
        repository.sectionPages[1] = DataResult.Failure(AppError.Network)
        val vm = viewModel()

        assertEquals(AppError.Network, vm.state.error)

        repository.banners = DataResult.Success(emptyList())
        repository.sectionPages[1] = page(section("a"), next = null)
        vm.retry()

        assertNull(vm.state.error)
        assertEquals(listOf("a"), vm.state.sections.map { it.id })
    }

    @Test
    fun givenOnlyBannersFail_whenOpened_thenRowsStillShow() {
        repository.banners = DataResult.Failure(AppError.Server(500))
        val vm = viewModel()

        assertNull(vm.state.error)
        assertTrue(vm.state.banners.isEmpty())
        assertEquals(2, vm.state.sections.size)
    }

    @Test
    fun givenRowsFailButBannersLoad_whenOpened_thenShowsBannersWithRetryForRows() {
        repository.sectionPages[1] = DataResult.Failure(AppError.Server(503))
        val vm = viewModel()

        assertNull(vm.state.error)
        assertEquals(1, vm.state.banners.size)
        assertEquals(PagingStatus.Failed(AppError.Server(503)), vm.state.paging)

        repository.sectionPages[1] = page(section("a"), next = null)
        vm.loadNextPage()
        assertEquals(listOf("a"), vm.state.sections.map { it.id })
    }

    @Test
    fun givenNoContentAtAll_whenOpened_thenShowsEmptyState() {
        repository.banners = DataResult.Success(emptyList())
        repository.sectionPages[1] = page(next = null)
        val vm = viewModel()

        assertTrue(vm.state.isEmpty)
    }

    @Test
    fun givenRowWithMoreItems_whenUserScrollsRow_thenItemsAppendAndNextPageAdvances() {
        repository.itemPages["b" to 2] = DataResult.Success(SectionItemsPage(listOf(item("b-new"), item("b-0")), nextPage = null))
        val vm = viewModel()

        vm.loadMoreItems("b")

        val row = vm.state.sections.first { it.id == "b" }
        assertEquals(listOf("b-0", "b-1", "b-2", "b-new"), row.items.map { it.id })
        assertNull(row.nextItemsPage)
        assertTrue(vm.state.loadingSectionIds.isEmpty())

        vm.loadMoreItems("b")
        assertEquals("Complete rows do not request again", listOf("b" to 2), repository.itemCalls)
    }

    @Test
    fun givenRowWithoutMoreItems_whenScrolled_thenNoRequest() {
        viewModel().loadMoreItems("a")
        assertTrue(repository.itemCalls.isEmpty())
    }

    @Test
    fun givenContentShown_whenRefreshFails_thenKeepsExistingContent() {
        val vm = viewModel()
        repository.sectionPages[1] = DataResult.Failure(AppError.Network)

        vm.refresh()

        assertFalse(vm.state.isRefreshing)
        assertEquals(listOf("a", "b"), vm.state.sections.map { it.id })
    }

    @Test
    fun givenContentShown_whenRefreshSucceeds_thenReplacesRows() {
        val vm = viewModel()
        vm.loadNextPage()
        repository.sectionPages[1] = page(section("z"), next = null)

        vm.refresh()

        assertEquals(listOf("z"), vm.state.sections.map { it.id })
        assertEquals(PagingStatus.EndReached, vm.state.paging)
    }
}
