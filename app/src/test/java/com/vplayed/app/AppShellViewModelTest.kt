package com.vplayed.app

import com.vplayed.R
import com.vplayed.shared.auth.LogoutResult
import com.vplayed.shared.core.error.AppError
import com.vplayed.shared.session.SessionStatus
import com.vplayed.shared.session.SignOutReason
import com.vplayed.testing.FakeAuthRepository
import com.vplayed.testing.MainDispatcherRule
import com.vplayed.testing.testAuthSession
import com.vplayed.testing.testSessionManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppShellViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeAuthRepository()
    private val sessions = testSessionManager(signedIn = testAuthSession)

    private fun viewModel() = AppShellViewModel(sessions, repository)

    @Test
    fun givenSignedIn_whenCreated_thenMenuShowsTheEmail() {
        assertEquals("user@example.com", viewModel().signedInEmail.value)
    }

    @Test
    fun givenLogoutAccepted_whenSigningOut_thenSessionEndsAndMenuShowsLogin() = runTest(mainDispatcherRule.testDispatcher) {
        val vm = viewModel()

        vm.signOut()
        runCurrent()

        assertEquals(1, repository.logouts)
        assertEquals(SessionStatus.SignedOut(SignOutReason.USER), sessions.status.value)
        assertNull(vm.signedInEmail.value)
        assertFalse(vm.isSigningOut.value)
    }

    @Test
    fun givenTokenAlreadyRejected_whenSigningOut_thenSessionEndsWithoutMessage() = runTest(mainDispatcherRule.testDispatcher) {
        repository.logoutResult = LogoutResult.SessionAlreadyEnded
        val vm = viewModel()

        vm.signOut()
        runCurrent()

        assertNull(sessions.session)
        assertNull(vm.signedInEmail.value)
    }

    @Test
    fun givenNoConnection_whenSigningOut_thenStaysSignedInAndExplains() = runTest(mainDispatcherRule.testDispatcher) {
        repository.logoutResult = LogoutResult.Failure(AppError.Network)
        val vm = viewModel()

        vm.signOut()
        runCurrent()

        assertEquals(testAuthSession, sessions.session)
        assertEquals("user@example.com", vm.signedInEmail.value)
        assertEquals(R.string.error_network, vm.messages.first())
    }

    @Test
    fun givenServerRefusal_whenSigningOut_thenStaysSignedInWithTheAppMessage() = runTest(mainDispatcherRule.testDispatcher) {
        repository.logoutResult = LogoutResult.Rejected("internal detail")
        val vm = viewModel()

        vm.signOut()
        runCurrent()

        assertEquals(testAuthSession, sessions.session)
        assertEquals(R.string.menu_sign_out_failed, vm.messages.first())
    }

    @Test
    fun givenSignOutInFlight_whenTappedAgain_thenOneRequest() = runTest(mainDispatcherRule.testDispatcher) {
        val hold = CompletableDeferred<Unit>()
        repository.logoutHold = hold
        val vm = viewModel()

        vm.signOut()
        runCurrent()
        assertTrue(vm.isSigningOut.value)
        vm.signOut()
        hold.complete(Unit)
        runCurrent()

        assertEquals(1, repository.logouts)
        assertFalse(vm.isSigningOut.value)
    }
}
