package com.vplayed.core.session

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.io.encoding.Base64

/** Encrypts the stored session. Both directions return null instead of throwing. */
interface SessionCipher {
    fun encrypt(plain: String): String?

    /** Null when [encoded] is not valid ciphertext for this device's key (corrupt, or the key was lost). */
    fun decrypt(encoded: String): String?
}

/**
 * AES-256-GCM with a key that lives in the Android Keystore and never leaves it. Output is
 * Base64 of `iv (12 bytes) + ciphertext + tag`.
 */
@Singleton
class KeystoreSessionCipher @Inject constructor() : SessionCipher {

    override fun encrypt(plain: String): String? = try {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, secretKey()) }
        Base64.Default.encode(cipher.iv + cipher.doFinal(plain.encodeToByteArray()))
    } catch (_: GeneralSecurityException) {
        null
    }

    override fun decrypt(encoded: String): String? = try {
        val bytes = Base64.Default.decode(encoded)
        require(bytes.size > IV_LENGTH)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(TAG_BITS, bytes, 0, IV_LENGTH))
        }
        cipher.doFinal(bytes, IV_LENGTH, bytes.size - IV_LENGTH).decodeToString()
    } catch (_: GeneralSecurityException) {
        null
    } catch (_: IllegalArgumentException) {
        // Not Base64 / too short.
        null
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).run {
            init(
                KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(KEY_BITS)
                    .build(),
            )
            generateKey()
        }
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "vplayed_session_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_BITS = 256
        const val IV_LENGTH = 12
        const val TAG_BITS = 128
    }
}
