package com.vplayed.core.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.vplayed.core.dispatcher.IoDispatcher
import com.vplayed.core.session.di.SessionPreferences
import com.vplayed.shared.session.SecureKey
import com.vplayed.shared.session.SecureStore
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * The shared layer's [SecureStore]: each key is one value in the `session` DataStore file
 * (excluded from backups), encrypted by [SessionCipher] (Keystore AES-GCM). Same file, preference
 * key and Keystore alias as the app used before the shared `SessionManager`, which reads the old
 * session JSON, so signed-in users stay signed in after the upgrade.
 *
 * Nothing is ever stored in plain text: without the Keystore a write fails and the session lasts
 * for this run only. A value this device cannot decrypt is removed and reads as absent.
 */
@Singleton
class KeystoreSecureStore @Inject constructor(
    @param:SessionPreferences private val dataStore: DataStore<Preferences>,
    private val cipher: SessionCipher,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : SecureStore {

    override suspend fun read(key: SecureKey): String? = withContext(ioDispatcher) {
        val preferences = dataStore.data
            .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
            .first()
        val stored = preferences[key.preference] ?: return@withContext null
        cipher.decrypt(stored) ?: run {
            delete(key)
            null
        }
    }

    override suspend fun write(key: SecureKey, value: String): Boolean = withContext(ioDispatcher) {
        val encrypted = cipher.encrypt(value) ?: return@withContext false
        try {
            dataStore.edit { it[key.preference] = encrypted }
            true
        } catch (_: IOException) {
            false
        }
    }

    override suspend fun delete(key: SecureKey) = withContext(ioDispatcher) {
        try {
            dataStore.edit { it.remove(key.preference) }
        } catch (_: IOException) {
            // Nothing to remove, or the file is unreadable: either way nothing can be restored from it.
        }
        Unit
    }

    private val SecureKey.preference: Preferences.Key<String>
        get() = when (this) {
            SecureKey.SESSION -> SESSION_KEY
        }

    companion object {
        /** The key the session has always been stored under. */
        val SESSION_KEY = stringPreferencesKey("session")
    }
}
