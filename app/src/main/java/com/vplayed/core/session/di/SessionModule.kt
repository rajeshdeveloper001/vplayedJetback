package com.vplayed.core.session.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.vplayed.core.dispatcher.IoDispatcher
import com.vplayed.core.session.DataStoreSessionRepository
import com.vplayed.core.session.KeystoreSessionCipher
import com.vplayed.core.session.SessionCipher
import com.vplayed.core.session.SessionRepository
import dagger.Binds
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

/** Replaced by an in-memory fake in instrumented tests (`TestModules.kt`). */
@Module
@InstallIn(SingletonComponent::class)
abstract class SessionRepositoryModule {
    @Binds
    abstract fun bindSessionRepository(impl: DataStoreSessionRepository): SessionRepository
}
