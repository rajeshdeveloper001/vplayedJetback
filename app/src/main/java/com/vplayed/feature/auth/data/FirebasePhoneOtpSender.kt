package com.vplayed.feature.auth.data

import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.vplayed.core.common.ActivityTracker
import com.vplayed.core.common.config.AppConfig
import com.vplayed.shared.auth.PhoneCodeCheckResult
import com.vplayed.shared.auth.PhoneCodeFailure
import com.vplayed.shared.auth.PhoneCodeSendResult
import com.vplayed.shared.auth.PhoneOtpSender
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The shared layer's [PhoneOtpSender] over Firebase Auth, as the previous apps send and check
 * phone codes. The backend never sees the code: once Firebase accepts it, the shared
 * `OtpController` calls `auth/phone-login`. Resends reuse Firebase's resend token per number.
 * Every callback fires once (instant verification after "code sent" is ignored).
 */
@Singleton
class FirebasePhoneOtpSender @Inject constructor(
    private val config: AppConfig,
    private val activities: ActivityTracker,
) : PhoneOtpSender {

    private val resendTokens = ConcurrentHashMap<String, PhoneAuthProvider.ForceResendingToken>()

    override fun sendCode(phoneNumber: String, onResult: (PhoneCodeSendResult) -> Unit) {
        val activity = activities.current
        if (config.firebase == null || activity == null) {
            onResult(PhoneCodeSendResult.Failed(PhoneCodeFailure.NOT_CONFIGURED))
            return
        }
        val delivered = AtomicBoolean(false)
        val reply = { result: PhoneCodeSendResult -> if (delivered.compareAndSet(false, true)) onResult(result) }
        val auth = FirebaseAuth.getInstance()
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                auth.signInWithCredential(credential).addOnCompleteListener { task ->
                    auth.signOut()
                    reply(if (task.isSuccessful) PhoneCodeSendResult.VerifiedAutomatically else PhoneCodeSendResult.Failed(PhoneCodeFailure.OTHER))
                }
            }

            override fun onVerificationFailed(e: FirebaseException) {
                reply(PhoneCodeSendResult.Failed(e.toFailure()))
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                resendTokens[phoneNumber] = token
                reply(PhoneCodeSendResult.CodeSent(verificationId))
            }
        }
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
        resendTokens[phoneNumber]?.let(options::setForceResendingToken)
        PhoneAuthProvider.verifyPhoneNumber(options.build())
    }

    override fun verifyCode(verificationId: String, code: String, onResult: (PhoneCodeCheckResult) -> Unit) {
        if (config.firebase == null) {
            onResult(PhoneCodeCheckResult.Failed(PhoneCodeFailure.NOT_CONFIGURED))
            return
        }
        val auth = FirebaseAuth.getInstance()
        auth.signInWithCredential(PhoneAuthProvider.getCredential(verificationId, code)).addOnCompleteListener { task ->
            val error = task.exception
            onResult(
                when {
                    task.isSuccessful -> {
                        // Only the check was needed; the backend session is the account's session.
                        auth.signOut()
                        PhoneCodeCheckResult.Verified
                    }
                    error is FirebaseAuthInvalidCredentialsException -> PhoneCodeCheckResult.InvalidCode
                    else -> PhoneCodeCheckResult.Failed((error as? FirebaseException)?.toFailure() ?: PhoneCodeFailure.OTHER)
                },
            )
        }
    }

    private fun FirebaseException.toFailure(): PhoneCodeFailure = when (this) {
        is FirebaseAuthInvalidCredentialsException -> PhoneCodeFailure.INVALID_NUMBER
        is FirebaseTooManyRequestsException -> PhoneCodeFailure.TOO_MANY_REQUESTS
        is FirebaseNetworkException -> PhoneCodeFailure.NETWORK
        else -> PhoneCodeFailure.OTHER
    }

    private companion object {
        /** Firebase's auto-retrieval window, matching the 60 s resend wait. */
        const val TIMEOUT_SECONDS = 60L
    }
}
