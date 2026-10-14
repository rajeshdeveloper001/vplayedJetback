package com.vplayed.core.security

import com.vplayed.testing.TransportCrypto
import com.vplayed.testing.TransportCrypto.TEST_PASSPHRASE
import kotlin.io.encoding.Base64
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CryptoTransportTest {

    private val fixedSalt = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)

    @Test
    fun givenFixedSalt_whenEncrypting_thenMatchesOpenSslPassphraseFormat() {
        val crypto = CryptoTransport { fixedSalt }

        val encrypted = requireNotNull(crypto.encryptForTransport(TEST_PASSPHRASE, PLAIN))

        val bytes = Base64.Default.decode(encrypted)
        assertEquals("Salted__", bytes.copyOfRange(0, 8).decodeToString())
        assertArrayEquals(fixedSalt, bytes.copyOfRange(8, 16))
        // Reference: printf '%s' '{"email":"user@example.com"}' |
        //   openssl enc -aes-256-cbc -md md5 -S 0102030405060708 -pass pass:test-passphrase -base64 -A
        // (OpenSSL 3 omits the Salted__ header when -S is given, so compare the ciphertext part.)
        assertArrayEquals(Base64.Default.decode(OPENSSL_CIPHERTEXT), bytes.copyOfRange(16, bytes.size))
    }

    @Test
    fun givenRandomSalt_whenEncryptingTwice_thenOutputsDifferButBothDecrypt() {
        val crypto = CryptoTransport()

        val first = requireNotNull(crypto.encryptForTransport(TEST_PASSPHRASE, PLAIN))
        val second = requireNotNull(crypto.encryptForTransport(TEST_PASSPHRASE, PLAIN))

        assertNotEquals(first, second)
        assertEquals(PLAIN, TransportCrypto.decrypt(TEST_PASSPHRASE, first))
        assertEquals(PLAIN, TransportCrypto.decrypt(TEST_PASSPHRASE, second))
        assertEquals(false, first.contains('\n'))
    }

    @Test
    fun givenEmptyPassphrase_whenEncrypting_thenReturnsNullInsteadOfEmptyPayload() {
        assertNull(CryptoTransport().encryptForTransport("", PLAIN))
    }

    private companion object {
        const val PLAIN = """{"email":"user@example.com"}"""
        const val OPENSSL_CIPHERTEXT = "SvRX3DFJS6tbD2rL98ARSk2wCRy4uv4h6lVQg3PEqjo="
    }
}
