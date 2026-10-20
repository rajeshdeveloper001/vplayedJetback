package com.vplayed.feature.auth.presentation.viewmodel

import com.vplayed.core.common.config.AppConfig
import com.vplayed.feature.auth.domain.SocialLoginHandoff
import com.vplayed.shared.auth.PhoneAuthRepository
import com.vplayed.shared.auth.PhoneOtpSender
import com.vplayed.shared.auth.RegistrationRepository
import com.vplayed.shared.auth.SocialAuthRepository
import com.vplayed.shared.profile.SessionProfileChooser
import com.vplayed.shared.settings.CommonSettingsStore
import javax.inject.Inject

/**
 * What phone and Google sign-in, sign-up and the profile choice add to the Login and OTP ViewModels
 * (one parameter, so their tests stay small).
 */
class AuthChannels @Inject constructor(
    val phone: PhoneAuthRepository,
    val social: SocialAuthRepository,
    val sender: PhoneOtpSender,
    val socialHandoff: SocialLoginHandoff,
    val settings: CommonSettingsStore,
    val config: AppConfig,
    /** Sign-up of new accounts (`SharedGraph.registrationRepository`). */
    val registration: RegistrationRepository,
    /** The account's first profile for each sign-in (`SharedGraph.profileChooser`). */
    val profileChooser: SessionProfileChooser,
)
