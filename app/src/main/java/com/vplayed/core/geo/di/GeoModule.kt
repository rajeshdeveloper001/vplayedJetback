package com.vplayed.core.geo.di

import com.vplayed.core.geo.data.GeoRestrictionApi
import com.vplayed.core.geo.data.PublicIpApi
import com.vplayed.core.geo.work.GeoRefreshScheduler
import com.vplayed.core.geo.work.WorkManagerGeoRefreshScheduler
import com.vplayed.core.network.ThirdPartyHttpClient
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object GeoApiModule {

    @Provides
    @Singleton
    fun providePublicIpApi(@ThirdPartyHttpClient client: OkHttpClient, json: Json): PublicIpApi =
        Retrofit.Builder()
            // The endpoint URL is absolute; the base URL only satisfies Retrofit.
            .baseUrl(PUBLIC_IP_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(PublicIpApi::class.java)

    @Provides
    @Singleton
    fun provideGeoRestrictionApi(retrofit: Retrofit): GeoRestrictionApi = retrofit.create(GeoRestrictionApi::class.java)

    private const val PUBLIC_IP_BASE_URL = "https://api.ipify.org/"
}

@Module
@InstallIn(SingletonComponent::class)
abstract class GeoBindingsModule {
    @Binds
    abstract fun bindGeoRefreshScheduler(scheduler: WorkManagerGeoRefreshScheduler): GeoRefreshScheduler
}
