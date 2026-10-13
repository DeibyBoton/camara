package com.xperiaprocam.core.imageprocessing

/**
 * Pure-Kotlin "food" look: warm white balance, mild saturation and contrast
 * lift. Applied per pixel on ARGB arrays; no Android dependencies so it is
 * unit-testable.
 */
object FoodFilter {

    fun apply(src: IntArray): IntArray {
        val out = IntArray(src.size)
        for (i in src.indices) {
            val p = src[i]
            var r = (p shr 16) and 0xFF
            var g = (p shr 8) and 0xFF
            var b = p and 0xFF

            // Warm shift: lift red, drop blue slightly.
            r = r + 10
            b = ((b * 46) / 50).coerceIn(0, 255)

            // Contrast around mid-gray.
            r = contrast(r); g = contrast(g); b = contrast(b)

            // Saturation boost toward luma.
            val luma = (r * 299 + g * 587 + b * 114) / 1000
            r = sat(r, luma); g = sat(g, luma); b = sat(b, luma)

            out[i] = (0xFF shl 24) or
                (r.coerceIn(0, 255) shl 16) or (g.coerceIn(0, 255) shl 8) or b.coerceIn(0, 255)
        }
        return out
    }

    private fun contrast(v: Int): Int = (v * 108 - 255 * 4) / 100

    private fun sat(v: Int, luma: Int): Int = luma + ((v - luma) * 118) / 100
}
