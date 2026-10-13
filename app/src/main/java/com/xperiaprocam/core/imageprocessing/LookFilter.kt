package com.xperiaprocam.core.imageprocessing

import com.xperiaprocam.domain.model.CaptureLook

/**
 * Creative look profiles (post-capture, per pixel, pure Kotlin):
 * NATURAL identity · VIVID saturation+contrast · MONO luminance · WARM warm shift.
 */
object LookFilter {

    fun apply(look: CaptureLook, src: IntArray): IntArray = when (look) {
        CaptureLook.NATURAL -> src.copyOf()
        CaptureLook.VIVID -> transform(src, contrast = 116, saturation = 130)
        CaptureLook.MONO -> transform(src, contrast = 104, saturation = 0)
        CaptureLook.WARM -> transform(src, contrast = 104, saturation = 108, warmShift = 12)
    }

    private fun transform(
        src: IntArray,
        contrast: Int,          // 100 = neutral
        saturation: Int,        // 0 = grayscale, 100 = neutral
        warmShift: Int = 0,     // + lifts red / drops blue
    ): IntArray {
        val out = IntArray(src.size)
        for (i in src.indices) {
            val p = src[i]
            var r = (p shr 16) and 0xFF
            var g = (p shr 8) and 0xFF
            var b = p and 0xFF

            if (warmShift != 0) {
                r += warmShift
                b = b - warmShift / 2
            }

            // Contrast around mid-gray.
            r = (r * contrast - 128 * (contrast - 100)) / 100
            g = (g * contrast - 128 * (contrast - 100)) / 100
            b = (b * contrast - 128 * (contrast - 100)) / 100

            // Saturation toward luma.
            val luma = (r.coerceIn(0, 255) * 299 + g.coerceIn(0, 255) * 587 + b.coerceIn(0, 255) * 114) / 1000
            r = luma + ((r - luma) * saturation) / 100
            g = luma + ((g - luma) * saturation) / 100
            b = luma + ((b - luma) * saturation) / 100

            out[i] = (0xFF shl 24) or
                (r.coerceIn(0, 255) shl 16) or (g.coerceIn(0, 255) shl 8) or b.coerceIn(0, 255)
        }
        return out
    }
}
