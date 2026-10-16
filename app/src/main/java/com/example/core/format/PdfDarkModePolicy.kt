package com.example.core.format

/**
 * Loop 12 dark-mode policy for the bundled PDF reader (Bug D).
 *
 * Reported symptom: human photos came out looking like film negatives in dark mode.
 * Root cause: the reader applied a whole-canvas `invert(1) hue-rotate(180deg)` filter
 * to every page, guarded only by a saturation-ratio `.photo` heuristic. A single
 * canvas contains the entire rendered page, so a CSS filter cannot selectively
 * invert text while preserving embedded images — and the heuristic missed
 * grayscale/low-saturation portraits entirely (pdf.js issues #11075/#8253 discuss
 * the same limitation upstream; separating text/vector from raster layers is not
 * safely possible in this architecture without parsing operator lists per page).
 *
 * Decision (safety over aggressive inversion):
 *  - A page is inverted ONLY when it is confidently a near-binary text page:
 *    almost no colorful pixels, almost no mid-tone pixels, and dominated by light
 *    pixels. Such a page cannot contain a photograph by construction.
 *  - EVERYTHING else — color photos, grayscale/low-saturation portraits, mixed
 *    text+photo pages, charts, scans, diagrams — keeps its original colors and is
 *    only slightly dimmed. The failure direction is now "some pages stay lighter
 *    than ideal", never "a portrait gets ghosted".
 *
 * The bundled reader (`search.html`) mirrors these thresholds in its page classifier
 * after each render; keep the two implementations in sync when tuning. Pages are
 * classified from their rendered bitmap, so the decision is theme-independent.
 */
object PdfDarkModePolicy {

    data class ToneStats(
        val colorfulRatio: Double,
        val midtoneRatio: Double,
        val lightRatio: Double
    )

    /** Sampled pixels whose channel spread exceeds the colorfulness threshold. */
    const val MAX_COLORFUL_RATIO: Double = 0.015

    /** Sampled pixels that are neither near-white nor near-black (continuous tone). */
    const val MAX_MIDTONE_RATIO: Double = 0.12

    /** Sampled pixels at or above the near-white luminance floor. */
    const val MIN_LIGHT_RATIO: Double = 0.55

    /** Pixel classification bands mirrored by the reader's sampler. */
    const val MIDTONE_LUMA_MIN: Int = 56
    const val MIDTONE_LUMA_MAX_EXCLUSIVE: Int = 200
    const val LIGHT_LUMA_MIN: Int = 200
    const val COLORFUL_CHANNEL_SPREAD_MIN: Int = 40

    /**
     * True when the page may be inverted for dark reading. Anything uncertain
     * returns false (natural colors + slight dim), which is the safe direction.
     */
    fun shouldInvert(stats: ToneStats): Boolean {
        if (!stats.colorfulRatio.isFinite() || !stats.midtoneRatio.isFinite() || !stats.lightRatio.isFinite()) {
            return false
        }
        return stats.colorfulRatio <= MAX_COLORFUL_RATIO &&
            stats.midtoneRatio <= MAX_MIDTONE_RATIO &&
            stats.lightRatio >= MIN_LIGHT_RATIO
    }

    /**
     * Classifies one downsampled page bitmap. [argbPixels] holds packed ARGB ints as
     * produced by Android bitmaps; counts mirror the reader's JS sampler exactly:
     * colorful = channel spread > 40; light = luma >= 200; midtone = the remainder
     * between 56 and 200. Unknown/transparent pixels count as colorful (safe).
     */
    fun classify(argbPixels: IntArray): ToneStats {
        val total = argbPixels.size
        if (total == 0) return ToneStats(1.0, 1.0, 0.0)
        var colorful = 0
        var light = 0
        var midtone = 0
        for (pixel in argbPixels) {
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            val spread = maxOf(r, g, b) - minOf(r, g, b)
            val luma = (r + g + b) / 3
            when {
                spread > COLORFUL_CHANNEL_SPREAD_MIN -> colorful++
                luma >= LIGHT_LUMA_MIN -> light++
                luma >= MIDTONE_LUMA_MIN -> midtone++
                else -> { /* near-black: text ink, not a risk signal */ }
            }
        }
        return ToneStats(
            colorfulRatio = colorful.toDouble() / total,
            midtoneRatio = midtone.toDouble() / total,
            lightRatio = light.toDouble() / total
        )
    }
}
