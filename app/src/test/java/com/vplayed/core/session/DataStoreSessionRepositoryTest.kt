package com.vplayed.core.session

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.vplayed.testing.Fixtures
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

@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreSessionRepositoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val testScope = TestScope(UnconfinedTestDispatcher())
    private val dataStore = PreferenceDataStoreFactory.create(
        scope = testScope.backgroundScope,
        produceFile = { File(tmp.root, "session_test.preferences_pb") },
    )
    private val sessionKey = stringPreferencesKey("session")
    private val cipher = FakeSessionCipher()
    private val session = UserSession("u1", "user@example.com", "Sam", "access-1", "refresh-1", "profile-1")

    private fun repository() = DataStoreSessionRepository(dataStore, cipher, Fixtures.json, testScope.backgroundScope)

    @Test
    fun givenNothingStored_whenLoaded_thenSignedOut() = testScope.runTest {
        val repository = repository()
        repository.awaitLoaded()
        assertNull(repository.session.value)
    }

    @Test
    fun givenSavedSession_whenNewRepositoryLoads_thenSessionRestored() = testScope.runTest {
        repository().save(session)

        val restored = repository()
        restored.awaitLoaded()

        assertEquals(session, restored.session.value)
    }

    @Test
    fun givenSavedSession_whenInspectingStorage_thenOnlyCiphertextIsStored() = testScope.runTest {
        repository().save(session)

        val raw = dataStore.data.first()[sessionKey]

        assertTrue(raw!!.startsWith(FakeSessionCipher.PREFIX))
        assertFalse("access-1" in raw)
        assertFalse("user@example.com" in raw)
    }

    @Test
    fun givenUndecryptableValue_whenLoaded_thenSignedOutAndValueCleared() = testScope.runTest {
        dataStore.edit { it[sessionKey] = "garbage-from-another-device" }

        val repository = repository()
        repository.awaitLoaded()

        assertNull(repository.session.value)
        assertNull(dataStore.data.first()[sessionKey])
    }

    @Test
    fun givenMalformedJsonInsideCiphertext_whenLoaded_thenSignedOut() = testScope.runTest {
        dataStore.edit { it[sessionKey] = cipher.encrypt("{not json")!! }

        val repository = repository()
        repository.awaitLoaded()

        assertNull(repository.session.value)
    }

    @Test
    fun givenKeystoreUnavailable_whenSaving_thenKeptInMemoryButNeverStored() = testScope.runTest {
        cipher.available = false
        val repository = repository()

        repository.save(session)

        assertEquals(session, repository.session.value)
        assertNull(dataStore.data.first()[sessionKey])
    }

    @Test
    fun givenSession_whenPrinted_thenTokensAndEmailAreMasked() {
        val text = session.toString()
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
