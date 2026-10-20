package com.vplayed.feature.auth.data

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.vplayed.core.common.config.AppConfig
import com.vplayed.shared.auth.JwtClaims
import com.vplayed.shared.auth.SocialCredential
import com.vplayed.shared.auth.SocialProvider
import javax.inject.Inject

/** What the Google sheet ended with. */
sealed interface GoogleSignInOutcome {
    data class Credential(val credential: SocialCredential) : GoogleSignInOutcome

    /** The user closed the sheet: nothing to report. */
    data object Cancelled : GoogleSignInOutcome

    data object Failed : GoogleSignInOutcome
}

/**
 * Google sign-in through Credential Manager ("Sign in with Google"). It returns Google's ID token,
 * which goes to `auth/login` as `id_token` with the account's `sub` as `social_user_id` (the iOS
 * app sends the OAuth access token instead; the backend must accept both).
 */
class GoogleSignInClient @Inject constructor(private val config: AppConfig) {

    /** False when the build has no web client ID: the Google button is hidden. */
    val isAvailable: Boolean get() = config.googleWebClientId.isNotBlank()

    suspend fun signIn(activity: Activity): GoogleSignInOutcome {
        if (!isAvailable) return GoogleSignInOutcome.Failed
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(config.googleWebClientId).build())
            .build()
        return try {
            val credential = CredentialManager.create(activity).getCredential(activity, request).credential
            if (credential !is CustomCredential || credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                android.util.Log.e("GoogleSignIn", "Unexpected credential type: ${credential::class.java.name}")
                return GoogleSignInOutcome.Failed
            }
            val google = GoogleIdTokenCredential.createFrom(credential.data)
            val uniqueId: String? = google.uniqueId
            val email: String? = google.email
            GoogleSignInOutcome.Credential(
                SocialCredential(
                    provider = SocialProvider.GOOGLE,
                    // The account's stable `sub`, as the ID token carries it.
                    socialUserId = uniqueId?.takeIf { it.isNotBlank() } ?: JwtClaims.string(google.idToken, "sub").orEmpty(),
                    token = google.idToken,
                    email = email,
                    name = google.displayName,
                    pictureUrl = google.profilePictureUri?.toString(),
                ),
            )
        } catch (_: GetCredentialCancellationException) {
            GoogleSignInOutcome.Cancelled
        } catch (e: GetCredentialException) {
            android.util.Log.e("GoogleSignIn", "GetCredentialException: [${e.type}] ${e.message}", e)
            GoogleSignInOutcome.Failed
        } catch (e: GoogleIdTokenParsingException) {
            android.util.Log.e("GoogleSignIn", "GoogleIdTokenParsingException: ${e.message}", e)
            GoogleSignInOutcome.Failed
        } catch (e: Exception) {
            android.util.Log.e("GoogleSignIn", "Unexpected exception during Google sign-in: ${e.message}", e)
            GoogleSignInOutcome.Failed
        }
    }
}
