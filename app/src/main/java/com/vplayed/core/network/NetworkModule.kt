package com.vplayed.core.network

import com.vplayed.core.network.connectivity.ConnectivityNetworkMonitor
import com.vplayed.core.network.connectivity.NetworkMonitor
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.OkHttpClient

/**
 * Networking foundation. Backend calls run on the KMP shared layer (`SharedModule`), whose Ktor
 * client uses this [OkHttpClient] as its engine and adds the VPlayed headers and its own redacting
 * log, so the client carries no interceptors.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder().build()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkBindingsModule {
    @Binds
    abstract fun bindDeviceIdProvider(provider: AndroidDeviceIdProvider): DeviceIdProvider

    @Binds
    abstract fun bindDeviceNameProvider(provider: AndroidDeviceNameProvider): DeviceNameProvider

    @Binds
    abstract fun bindNetworkMonitor(monitor: ConnectivityNetworkMonitor): NetworkMonitor
}
