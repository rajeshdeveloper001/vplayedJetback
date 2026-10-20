package com.vplayed.feature.auth.presentation.components

/** Test tags for Login UI tests. */
object LoginTestTags {
    const val SCREEN = "login_screen"
    const val CARD = "login_card"
    const val SHOWCASE = "login_showcase"
    const val EMAIL_FIELD = "login_email_field"
    const val EMAIL_ERROR = "login_email_error"
    const val PHONE_FIELD = "login_phone_field"
    const val PHONE_ERROR = "login_phone_error"
    const val GOOGLE = "login_google"
    const val SUBMIT = "login_submit"
    const val CLOSE = "login_close"
    const val GUEST = "login_guest"
}

/** Test tags for OTP UI tests. */
object OtpTestTags {
    const val SCREEN = "otp_screen"
    const val CARD = "otp_card"
    const val BACK = "otp_back"
    const val CHANGE = "otp_change"
    const val CODE_FIELD = "otp_code_field"
    const val VERIFY = "otp_verify"
    const val RESEND = "otp_resend"
    const val RESEND_COUNTDOWN = "otp_resend_countdown"
    const val ERROR = "otp_error"
    const val INFO = "otp_info"
    const val DEVICES = "otp_devices"
    const val DEVICE_SIGN_OUT_PREFIX = "otp_device_sign_out_"
    const val DEVICES_PRIMARY = "otp_devices_primary"
    const val SUCCESS = "otp_success"
    const val NOTICE = "otp_notice"
}

/** Test tags of the sign-up form (OTP screen, new accounts). */
object RegistrationTestTags {
    const val FORM = "register_form"
    const val NAME_FIELD = "register_name_field"
    const val NAME_ERROR = "register_name_error"
    const val EMAIL_FIELD = "register_email_field"
    const val EMAIL_ERROR = "register_email_error"
    const val READ_ONLY = "register_read_only"
    const val REFERRAL_TOGGLE = "register_referral_toggle"
    const val REFERRAL_FIELD = "register_referral_field"
    const val SUBMIT = "register_submit"
    const val ERROR = "register_error"
}
