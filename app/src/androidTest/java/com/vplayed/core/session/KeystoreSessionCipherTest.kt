package com.vplayed.core.session

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/** The real Android Keystore cipher, which unit tests cannot run. */
@RunWith(AndroidJUnit4::class)
class KeystoreSessionCipherTest {

    private val cipher = KeystoreSessionCipher()

    @Test
    fun givenPlainText_whenEncryptedAndDecrypted_thenRoundTrips() {
        val plain = """{"userId":"u1","accessToken":"secret-access"}"""

        val encrypted = cipher.encrypt(plain)

        assertEquals(plain, cipher.decrypt(encrypted!!))
        assertEquals(false, "secret-access" in encrypted)
    }

    @Test
    fun givenSamePlainText_whenEncryptedTwice_thenCiphertextsDiffer() {
        assertNotEquals(cipher.encrypt("same"), cipher.encrypt("same"))
    }

    @Test
    fun givenTamperedOrForeignCiphertext_whenDecrypting_thenNullInsteadOfCrash() {
        val encrypted = cipher.encrypt("secret")!!
        val tampered = encrypted.dropLast(6) + (if (encrypted.takeLast(6) == "AAAAAA") "BBBBBB" else "AAAAAA")

        assertNull(cipher.decrypt(tampered))
        assertNull(cipher.decrypt("not base64 at all %%%"))
        assertNull(cipher.decrypt(""))
        assertNull(cipher.decrypt("AAAA"))
    }
}
