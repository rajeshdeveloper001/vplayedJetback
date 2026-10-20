package com.vplayed.feature.auth.data.di

import com.vplayed.feature.auth.data.FirebasePhoneOtpSender
import com.vplayed.shared.SharedGraph
import com.vplayed.shared.auth.PhoneAuthRepository
import com.vplayed.shared.auth.PhoneOtpSender
import com.vplayed.shared.auth.RegistrationRepository
import com.vplayed.shared.auth.SocialAuthRepository
import com.vplayed.shared.profile.SessionProfileChooser
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Phone and Google sign-in, sign-up and the profile choice on the KMP shared layer
 * (`SharedGraph.phoneAuthRepository`, `socialAuthRepository`, `registrationRepository`,
 * `profileChooser`) and the Firebase code sender. Separate from [AuthRepositoryModule], which
 * instrumented tests replace.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AuthChannelModule {
    @Binds
    abstract fun bindPhoneOtpSender(sender: FirebasePhoneOtpSender): PhoneOtpSender

    companion object {
        @Provides
        fun providePhoneAuthRepository(graph: SharedGraph): PhoneAuthRepository = graph.phoneAuthRepository

        @Provides
        fun provideSocialAuthRepository(graph: SharedGraph): SocialAuthRepository = graph.socialAuthRepository

        @Provides
        fun provideRegistrationRepository(graph: SharedGraph): RegistrationRepository = graph.registrationRepository

        @Provides
        fun provideProfileChooser(graph: SharedGraph): SessionProfileChooser = graph.profileChooser
    }
}
