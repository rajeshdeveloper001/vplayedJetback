package com.vplayed.core.geo

import com.vplayed.testing.Fixtures
import kotlin.io.encoding.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GeoTokenDecoderTest {

    private val decoder = GeoTokenDecoder(Fixtures.json)

    private fun token(json: String) = Base64.encode(json.encodeToByteArray())

    @Test
    fun givenValidToken_whenDecoding_thenReturnsCurrency() {
        assertEquals("USD", decoder.decodeCurrency(token("""{"currency":"USD","country":"US"}""")))
    }

    @Test
    fun givenTokenWithoutPaddingOrWithLineBreaks_whenDecoding_thenStillReadsCurrency() {
        val padded = token("""{"currency":"EUR"}""")
        assertEquals("EUR", decoder.decodeCurrency(padded.trimEnd('=')))
        assertEquals("EUR", decoder.decodeCurrency(padded.chunked(8).joinToString("\n")))
    }

    @Test
    fun givenInvalidBase64_whenDecoding_thenReturnsNullWithoutCrashing() {
        assertNull(decoder.decodeCurrency("%%%not-base64%%%"))
    }

    @Test
    fun givenMalformedJson_whenDecoding_thenReturnsNullWithoutCrashing() {
        assertNull(decoder.decodeCurrency(token("""{"currency":""")))
        assertNull(decoder.decodeCurrency(token("plain text")))
        assertNull(decoder.decodeCurrency(token("""["USD"]""")))
    }

    @Test
    fun givenMissingOrBlankCurrency_whenDecoding_thenReturnsNull() {
        assertNull(decoder.decodeCurrency(token("""{"country":"IN"}""")))
        assertNull(decoder.decodeCurrency(token("""{"currency":"  "}""")))
        assertNull(decoder.decodeCurrency(token("""{"currency":null}""")))
    }

    @Test
    fun givenBlankToken_whenDecoding_thenReturnsNull() {
        assertNull(decoder.decodeCurrency(""))
        assertNull(decoder.decodeCurrency("  \n"))
    }
}
