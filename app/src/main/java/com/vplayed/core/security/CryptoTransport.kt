package com.vplayed.core.security

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import kotlin.io.encoding.Base64

/**
 * Request payload encryption expected by the VPlayed backend — a port of the previous app's
 * `CryptoTransport` (legacy `AESEncryption` path, the one it used for all requests):
 * CryptoJS/OpenSSL compatible "passphrase" AES.
 *
 * - key + IV = OpenSSL `EVP_BytesToKey(MD5, 1 iteration)` over passphrase and an 8-byte random salt
 * - AES-256-CBC with PKCS#5 padding
 * - output = Base64 (no line wrap) of `"Salted__" + salt + ciphertext`
 *
 * Do not change the algorithm without a matching backend change.
 */
class CryptoTransport internal constructor(private val saltSource: (Int) -> ByteArray) {

    @Inject
    constructor() : this(::randomSalt)

    /** Returns the encrypted text, or null when encryption is impossible (never an empty payload). */
    fun encryptForTransport(passphrase: String, plainText: String): String? {
        if (passphrase.isEmpty()) return null
        return try {
            val salt = saltSource(SALT_LENGTH)
            val (key, iv) = deriveKeyAndIv(passphrase.encodeToByteArray(), salt)
            val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, ALGORITHM), IvParameterSpec(iv))
            }
            val cipherText = cipher.doFinal(plainText.encodeToByteArray())
            Base64.Default.encode(SALTED_PREFIX + salt + cipherText)
        } catch (_: java.security.GeneralSecurityException) {
            null
        }
    }

    internal companion object {
        const val SALT_LENGTH = 8
        const val TRANSFORMATION = "AES/CBC/PKCS5Padding"
        const val ALGORITHM = "AES"
        private const val KEY_LENGTH = 32
        private const val IV_LENGTH = 16
        val SALTED_PREFIX = "Salted__".encodeToByteArray()

        /** OpenSSL EVP_BytesToKey with MD5 and one iteration: D_i = MD5(D_{i-1} + passphrase + salt). */
        fun deriveKeyAndIv(passphrase: ByteArray, salt: ByteArray): Pair<ByteArray, ByteArray> {
            val md5 = MessageDigest.getInstance("MD5")
            var derived = ByteArray(0)
            var block = ByteArray(0)
            while (derived.size < KEY_LENGTH + IV_LENGTH) {
                md5.update(block)
                md5.update(passphrase)
                block = md5.digest(salt)
                derived += block
            }
            return derived.copyOfRange(0, KEY_LENGTH) to derived.copyOfRange(KEY_LENGTH, KEY_LENGTH + IV_LENGTH)
        }

        private fun randomSalt(length: Int): ByteArray = ByteArray(length).also(SecureRandom()::nextBytes)
    }
}
