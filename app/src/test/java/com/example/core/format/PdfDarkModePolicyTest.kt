package com.example.core.format

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Loop 12 tests for the dark-mode page classifier (Bug D).
 *
 * Device symptom being guarded against: human photos turned into film negatives in
 * dark mode. The old whole-canvas inversion with a saturation-only `.photo` guard
 * missed grayscale/low-saturation portraits entirely. The policy now inverts ONLY
 * confidently near-binary text pages; everything else keeps its original colors.
 */
class PdfDarkModePolicyTest {

    private fun stats(
        colorfulPercent: Double,
        midtonePercent: Double,
        lightPercent: Double
    ): PdfDarkModePolicy.ToneStats = PdfDarkModePolicy.ToneStats(
        colorfulRatio = colorfulPercent / 100.0,
        midtoneRatio = midtonePercent / 100.0,
        lightRatio = lightPercent / 100.0
    )

    @Test
    fun `pure black on white text page is inverted`() {
        assertTrue(PdfDarkModePolicy.shouldInvert(stats(0.0, 0.5, 93.0)))
    }

    @Test
    fun `antialiased text page is still inverted`() {
        // Typical rendered text: ~5% antialiasing edge pixels spread across mid-tones.
        assertTrue(PdfDarkModePolicy.shouldInvert(stats(0.2, 6.0, 90.0)))
    }

    @Test
    fun `color portrait page stays natural`() {
        assertFalse(PdfDarkModePolicy.shouldInvert(stats(22.0, 30.0, 45.0)))
    }

    @Test
    fun `grayscale portrait stays natural`() {
        // Skin gradients are continuous-tone: colorful ~0 but heavy mid-tones.
        assertFalse(PdfDarkModePolicy.shouldInvert(stats(0.4, 46.0, 50.0)))
    }

    @Test
    fun `low saturation portrait stays natural`() {
        assertFalse(PdfDarkModePolicy.shouldInvert(stats(1.0, 52.0, 44.0)))
    }

    @Test
    fun `mixed text and photo page stays natural`() {
        assertFalse(PdfDarkModePolicy.shouldInvert(stats(6.0, 18.0, 70.0)))
    }

    @Test
    fun `chart with colored lines stays natural`() {
        assertFalse(PdfDarkModePolicy.shouldInvert(stats(4.0, 9.0, 85.0)))
    }

    @Test
    fun `grayscale scan of a photo stays natural`() {
        assertFalse(PdfDarkModePolicy.shouldInvert(stats(0.1, 38.0, 60.0)))
    }

    @Test
    fun `vector diagram with thin grey strokes stays natural`() {
        assertFalse(PdfDarkModePolicy.shouldInvert(stats(0.3, 14.0, 84.0)))
    }

    @Test
    fun `small colored logo on a text page may invert`() {
        // Up to 1.5% colorful pixels is still confidently text-only; logos are
        // graphics, not portraits, so this is the accepted (safe) edge case.
        assertTrue(PdfDarkModePolicy.shouldInvert(stats(1.2, 4.0, 92.0)))
    }

    @Test
    fun `uncertain statistics never invert`() {
        assertFalse(PdfDarkModePolicy.shouldInvert(PdfDarkModePolicy.ToneStats(Double.NaN, 0.0, 1.0)))
        assertFalse(PdfDarkModePolicy.shouldInvert(PdfDarkModePolicy.ToneStats(0.0, Double.NaN, 1.0)))
        assertFalse(PdfDarkModePolicy.shouldInvert(PdfDarkModePolicy.ToneStats(0.0, 0.0, Double.NaN)))
    }

    @Test
    fun `classify mirrors the reader sampler bands`() {
        // Build a 4x4 image: 12 white pixels, 3 black pixels, 1 red pixel.
        val white = 0xFFFFFFFF.toInt()
        val black = 0xFF000000.toInt()
        val red = 0xFFFF0000.toInt()
        val pixels = IntArray(16) { index ->
            when {
                index == 0 -> red
                index < 4 -> black
                else -> white
            }
        }
        val result = PdfDarkModePolicy.classify(pixels)
        assertEquals(1.0 / 16.0, result.colorfulRatio, 0.0001)
        assertEquals(0.0, result.midtoneRatio, 0.0001)
        assertEquals(12.0 / 16.0, result.lightRatio, 0.0001)
        // Red pixel exceeds the 1.5% colorful budget -> natural (safe direction).
        assertFalse(PdfDarkModePolicy.shouldInvert(result))
    }

    @Test
    fun `classify of an empty bitmap defaults to natural`() {
        val result = PdfDarkModePolicy.classify(IntArray(0))
        assertFalse(PdfDarkModePolicy.shouldInvert(result))
    }

    @Test
    fun `pure grayscale photo samples stay natural end to end`() {
        // Mid-grey pixels everywhere: no spread, all mid-tone.
        val grey = 0xFF808080.toInt()
        val result = PdfDarkModePolicy.classify(IntArray(64) { grey })
        assertFalse(PdfDarkModePolicy.shouldInvert(result))
    }
}
