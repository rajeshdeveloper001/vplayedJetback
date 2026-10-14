package com.vplayed.core.network

import com.vplayed.core.common.config.AppConfig
import com.vplayed.core.network.connectivity.ConnectivityNetworkMonitor
import com.vplayed.core.network.connectivity.NetworkMonitor
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Client for third-party services (e.g. the public IP lookup): no VPlayed headers, so device id
 * and geo token never leave for another host.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ThirdPartyHttpClient

/**
 * Networking foundation. Feature APIs are created from the [Retrofit] provided here in the owning
 * feature's data layer (`retrofit.create(MyApi::class.java)`).
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        // A null where the model has a default (e.g. "data": null) falls back to the default.
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(config: AppConfig, headers: ApiHeadersInterceptor): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(headers)
        .apply {
            // Logging is enabled per environment in app/build.gradle.kts; never in stage/demo/release.
            if (config.loggingEnabled) addInterceptor(SelectiveLoggingInterceptor())
        }
        .build()

    @Provides
    @Singleton
    @ThirdPartyHttpClient
    fun provideThirdPartyOkHttpClient(config: AppConfig, client: OkHttpClient): OkHttpClient =
        // Shares the connection pool and dispatcher, but none of the app interceptors.
        client.newBuilder()
            .apply {
                interceptors().clear()
                if (config.loggingEnabled) {
                    addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
                }
            }
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(config: AppConfig, client: OkHttpClient, json: Json): Retrofit = Retrofit.Builder()
        .baseUrl(config.serverUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
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
