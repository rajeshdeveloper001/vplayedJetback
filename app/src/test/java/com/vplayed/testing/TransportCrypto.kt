package com.vplayed.testing

import com.vplayed.core.security.CryptoTransport
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.Base64

/** Test-only inverse of [CryptoTransport.encryptForTransport] (what the backend does). */
object TransportCrypto {
    const val TEST_PASSPHRASE = "test-passphrase"

    fun decrypt(passphrase: String, encoded: String): String {
        val bytes = Base64.Default.decode(encoded)
        require(bytes.copyOfRange(0, 8).contentEquals(CryptoTransport.SALTED_PREFIX)) { "Missing Salted__ header" }
        val salt = bytes.copyOfRange(8, 16)
        val (key, iv) = CryptoTransport.deriveKeyAndIv(passphrase.encodeToByteArray(), salt)
        val cipher = Cipher.getInstance(CryptoTransport.TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, SecretKeySpec(key, CryptoTransport.ALGORITHM), IvParameterSpec(iv))
        }
        return cipher.doFinal(bytes.copyOfRange(16, bytes.size)).decodeToString()
    }
}
