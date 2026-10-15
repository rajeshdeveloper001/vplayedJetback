package com.vplayed.core.session.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.vplayed.core.dispatcher.IoDispatcher
import com.vplayed.core.session.KeystoreSecureStore
import com.vplayed.core.session.KeystoreSessionCipher
import com.vplayed.core.session.SessionCipher
import com.vplayed.shared.session.SessionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SessionPreferences

@Module
@InstallIn(SingletonComponent::class)
object SessionStorageModule {
    @Provides
    @Singleton
    @SessionPreferences
    fun provideSessionDataStore(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        scope = CoroutineScope(SupervisorJob() + ioDispatcher),
        produceFile = { context.preferencesDataStoreFile("session") },
    )

    @Provides
    fun provideSessionCipher(cipher: KeystoreSessionCipher): SessionCipher = cipher
}

/**
 * The shared layer's session (signed-in user, stored with [KeystoreSecureStore]), one per process.
 * Created here and passed to the `SharedGraph` (like the `GeoStore`), so screens and tests can use
 * it without the graph. Replaced by an in-memory one in instrumented tests (`TestModules.kt`).
 */
@Module
@InstallIn(SingletonComponent::class)
object SessionManagerModule {
    @Provides
    @Singleton
    fun provideSessionManager(store: KeystoreSecureStore): SessionManager = SessionManager(store)
}
