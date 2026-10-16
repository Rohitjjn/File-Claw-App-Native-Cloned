package com.example.core.format

/**
 * Device-aware per-page canvas pixel budget for the bundled PDF.js reader (Loop 10).
 *
 * The reader previously used one fixed 9M-pixel budget on every device. On high-density
 * phones (devicePixelRatio ~3) the backing pixels genuinely needed for a crisp page at
 * 3x pinch zoom exceed that budget, so the canvas was undersampled and zoomed text
 * stayed visibly blurry exactly inside the practical 2x-3x range.
 *
 * This policy derives a conservative budget from app heap signals only:
 *  - [ActivityManager.isLowRamDevice] flags entry-level devices;
 *  - [ActivityManager.memoryClass] is the **per-app Java heap cap in megabytes**. It is
 *    a heap headroom signal and must NOT be read as the phone's physical RAM size.
 * Canvas rasters are allocated by the WebView renderer process, but heap headroom is the
 * best offline proxy for what a device can sustain without renderer kills or OOM.
 */
object PdfRenderBudgetPolicy {

    /** Hard floor so even the weakest devices keep a meaningful re-render budget. */
    const val MIN_BUDGET_PIXELS: Long = 4_000_000L

    /** Hard ceiling; anything above this risks renderer termination on any device. */
    const val MAX_BUDGET_PIXELS: Long = 24_000_000L

    /** Budget used when no heap signal is available at all. */
    const val DEFAULT_BUDGET_PIXELS: Long = 12_000_000L

    /**
     * Maps device heap signals to a per-page canvas pixel budget (width x height of the
     * backing store, before the 4 bytes-per-pixel RGBA multiplication).
     */
    fun budgetPixels(isLowRamDevice: Boolean, memoryClassMb: Int?): Long = when {
        isLowRamDevice -> 6_000_000L
        memoryClassMb == null -> DEFAULT_BUDGET_PIXELS
        memoryClassMb >= 256 -> 16_000_000L
        memoryClassMb >= 128 -> 12_000_000L
        else -> 9_000_000L
    }

    /** Clamps any externally supplied budget into the safe range. */
    fun coerceBudget(pixels: Long): Long = pixels.coerceIn(MIN_BUDGET_PIXELS, MAX_BUDGET_PIXELS)
}
