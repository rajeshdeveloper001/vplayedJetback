package com.vplayed.feature.auth.presentation.viewmodel

import com.vplayed.shared.auth.AuthFlowConfig

/**
 * This app offers the device-limit step, including continuing without a code after a device is
 * signed out (`verifyEmailLogin`, decision D5; VPlayed keeps it off until the backend confirms it).
 */
internal val AUTH_FLOW_CONFIG = AuthFlowConfig(deviceSignOutEnabled = true)
