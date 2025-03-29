package space.iseki.ktunstrnorm

import kotlin.test.Test
import kotlin.test.assertEquals

class NormalizationTest {

    @Test
    fun testNFDNormalization() {
        val input = "é" // U+00E9
        val expected = "e\u0301" // U+0065 + U+0301
        val result = doConvert(input, NormalizationForm.NFD)
        assertEquals(expected, result, "NFD normalization failed")
    }

    @Test
    fun testNFCNormalization() {
        val input = "e\u0301" // U+0065 + U+0301
        val expected = "é" // U+00E9
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
        val input = "①" // U+2460
        val expected = "1"
        val result = doConvert(input, NormalizationForm.NFKC)
        assertEquals(expected, result, "NFKC normalization failed")
    }
}
