package com.vplayed.core.session

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import com.vplayed.shared.auth.AuthSession
import com.vplayed.shared.session.SecureKey
import com.vplayed.shared.session.SessionManager
import com.vplayed.shared.session.SessionStatus
import com.vplayed.shared.session.SignOutReason
import com.vplayed.testing.testAuthSession
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** [KeystoreSecureStore] under the shared [SessionManager], with a stand-in cipher (the real one runs on a device). */
@OptIn(ExperimentalCoroutinesApi::class)
class KeystoreSecureStoreTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val dispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(dispatcher)
    private val dataStore = PreferenceDataStoreFactory.create(
        scope = testScope.backgroundScope,
        produceFile = { File(tmp.root, "session_test.preferences_pb") },
    )
    private val sessionKey = KeystoreSecureStore.SESSION_KEY
    private val cipher = FakeSessionCipher()

    private fun store() = KeystoreSecureStore(dataStore, cipher, dispatcher)

    private fun sessions() = SessionManager(store())

    @Test
    fun givenNothingStored_whenLoaded_thenSignedOut() = testScope.runTest {
        val sessions = sessions()
        sessions.awaitLoaded()
        assertEquals(SessionStatus.SignedOut(reason = null), sessions.status.value)
    }

    @Test
    fun givenSessionStoredByThePreviousAppVersion_whenLoaded_thenTheUserStaysSignedIn() = testScope.runTest {
        // Exactly what DataStoreSessionRepository wrote: UserSession JSON, encrypted, under "session".
        val legacy = """{"userId":"u1","email":"user@example.com","displayName":"Sam","accessToken":"access-1","refreshToken":"refresh-1","profileId":"profile-1"}"""
        dataStore.edit { it[sessionKey] = cipher.encrypt(legacy)!! }

        val sessions = sessions()
        sessions.awaitLoaded()

        assertEquals(
            AuthSession("access-1", "refresh-1", "u1", null, "user@example.com", null, "Sam", "profile-1", isGuest = false),
            sessions.session,
        )
    }

    @Test
    fun givenSignIn_whenANewManagerLoads_thenSessionRestored() = testScope.runTest {
        sessions().signIn(testAuthSession)

        val restored = sessions()
        restored.awaitLoaded()

        assertEquals(testAuthSession, restored.session)
    }

    @Test
    fun givenSignIn_whenInspectingStorage_thenOnlyCiphertextIsStored() = testScope.runTest {
        sessions().signIn(testAuthSession)

        val raw = dataStore.data.first()[sessionKey]

        assertTrue(raw!!.startsWith(FakeSessionCipher.PREFIX))
        assertFalse("access-1" in raw)
        assertFalse("user@example.com" in raw)
    }

    @Test
    fun givenUndecryptableValue_whenLoaded_thenSignedOutAndValueCleared() = testScope.runTest {
        dataStore.edit { it[sessionKey] = "garbage-from-another-device" }

        val sessions = sessions()
        sessions.awaitLoaded()

        assertNull(sessions.session)
        assertNull(dataStore.data.first()[sessionKey])
    }

    @Test
    fun givenMalformedJsonInsideCiphertext_whenLoaded_thenSignedOutAndValueCleared() = testScope.runTest {
        dataStore.edit { it[sessionKey] = cipher.encrypt("{not json")!! }

        val sessions = sessions()
        sessions.awaitLoaded()

        assertNull(sessions.session)
        assertNull(dataStore.data.first()[sessionKey])
    }

    @Test
    fun givenKeystoreUnavailable_whenSigningIn_thenKeptInMemoryButNeverStored() = testScope.runTest {
        cipher.available = false
        val sessions = sessions()

        sessions.signIn(testAuthSession)

        assertEquals(testAuthSession, sessions.session)
        assertNull(dataStore.data.first()[sessionKey])
        assertFalse(store().write(SecureKey.SESSION, "value"))
    }

    @Test
    fun givenSession_whenSignedOut_thenStorageCleared() = testScope.runTest {
        val sessions = sessions()
        sessions.signIn(testAuthSession)

        sessions.signOut(SignOutReason.USER)

        assertNull(dataStore.data.first()[sessionKey])
        assertNull(sessions().also { it.awaitLoaded() }.session)
    }

    @Test
    fun givenSession_whenPrinted_thenTokensAreMasked() {
        val text = testAuthSession.toString()
        assertFalse("access-1" in text)
        assertFalse("refresh-1" in text)
        assertFalse("user@example.com" in text)
    }

    /** Reversible stand-in for the Keystore cipher (the real one is tested on a device). */
    private class FakeSessionCipher : SessionCipher {
        var available = true

        override fun encrypt(plain: String): String? = if (available) PREFIX + plain.reversed() else null

        override fun decrypt(encoded: String): String? =
            if (encoded.startsWith(PREFIX)) encoded.removePrefix(PREFIX).reversed() else null

        companion object {
            const val PREFIX = "enc:"
        }
    }
}
