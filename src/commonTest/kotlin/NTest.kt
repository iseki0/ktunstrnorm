package space.iseki.ktunstrnorm

import kotlin.test.Test
import kotlin.test.assertEquals

class NormalizationTest {

    @Test
    fun testNFDNormalization() {
        val input = "\u00E9"           // é = U+00E9
        val expected = "\u0065\u0301"  // e + ́ = U+0065 U+0301
        val result = doConvert(input, NormalizationForm.NFD)
        assertEquals(expected, result, "NFD normalization failed")
    }

    @Test
    fun testNFCNormalization() {
        val input = "\u0065\u0301"     // e + ́ = U+0065 U+0301
        val expected = "\u00E9"        // é = U+00E9
        val result = doConvert(input, NormalizationForm.NFC)
        assertEquals(expected, result, "NFC normalization failed")
    }

    @Test
    fun testNFKDNormalization() {
        val input = "ﬃ" // U+FB03 (ligature "ffi")
        val expected = "ffi"
        val result = doConvert(input, NormalizationForm.NFKD)
        assertEquals(expected, result, "NFKD normalization failed")
    }

    @Test
    fun testNFKCNormalization() {
        val input = "\u2460"           // ① = U+2460
        val expected = "1"             // compatibility: "1"
        val result = doConvert(input, NormalizationForm.NFKC)
        assertEquals(expected, result, "NFKC normalization failed")
    }

    @Test
    fun testEmpty() {
        assertEquals("", "".normalize(NormalizationForm.NFKC))
    }
}
