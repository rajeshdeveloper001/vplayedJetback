package com.vplayed.core.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.vplayed.core.dispatcher.ApplicationScope
import com.vplayed.core.session.di.SessionPreferences
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/**
 * Keeps the session as one encrypted JSON value in its own DataStore file (excluded from
 * backups). A value that cannot be decrypted or parsed means "signed out" and is cleared.
 */
@Singleton
class DataStoreSessionRepository @Inject constructor(
    @param:SessionPreferences private val dataStore: DataStore<Preferences>,
    private val cipher: SessionCipher,
    private val json: Json,
    @ApplicationScope scope: CoroutineScope,
) : SessionRepository {

    private val _session = MutableStateFlow<UserSession?>(null)
    override val session: StateFlow<UserSession?> = _session.asStateFlow()
    private val loaded = CompletableDeferred<Unit>()

    init {
        scope.launch {
            dataStore.data
                .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
                .collect { preferences ->
                    val stored = preferences[KEY_SESSION]
                    val restored = stored?.let(::decode)
                    if (stored != null && restored == null) clear()
                    _session.value = restored
                    loaded.complete(Unit)
                }
        }
    }

    override suspend fun awaitLoaded() = loaded.await()

    override suspend fun save(session: UserSession) {
        val encrypted = cipher.encrypt(json.encodeToString(UserSession.serializer(), session))
        if (encrypted == null) {
            // The Keystore is unavailable: keep the session for this run only, never store it plain.
            _session.value = session
            return
        }
        dataStore.edit { it[KEY_SESSION] = encrypted }
        _session.value = session
    }

    private fun decode(stored: String): UserSession? {
        val plain = cipher.decrypt(stored) ?: return null
        return try {
            json.decodeFromString(UserSession.serializer(), plain)
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private suspend fun clear() {
        dataStore.edit { it.remove(KEY_SESSION) }
    }

    private companion object {
        val KEY_SESSION = stringPreferencesKey("session")
    }
}
