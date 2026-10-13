package com.xperiaprocam.core.imageprocessing

/**
 * Pure-Kotlin per-pixel stacker: averages aligned frames. Pixels outside the
 * shifted frame bounds are skipped so no dark borders appear.
 */
class FrameMerger(private val width: Int, private val height: Int) : PixelMerger {

    private val n = width * height
    private val sumR = LongArray(n)
    private val sumG = LongArray(n)
    private val sumB = LongArray(n)
    private val count = IntArray(n)
    private var frames = 0

    /**
     * Adds a frame whose content satisfies frame(x + dx, y + dy) ≈ reference(x, y).
     */
    override fun add(pixels: IntArray, dx: Int, dy: Int) {
        require(pixels.size == n) { "frame size mismatch" }
        frames++
        for (y in 0 until height) {
            val fy = y + dy
            if (fy < 0 || fy >= height) continue
            val rowRef = y * width
            val rowFrame = fy * width
            for (x in 0 until width) {
                val fx = x + dx
                if (fx < 0 || fx >= width) continue
                val i = rowRef + x
                val p = pixels[rowFrame + fx]
                sumR[i] += (p shr 16) and 0xFF
                sumG[i] += (p shr 8) and 0xFF
                sumB[i] += p and 0xFF
                count[i]++
            }
        }
    }

    fun frameCount(): Int = frames

    /** @return merged ARGB pixels; 0xFF000000 alpha everywhere. */
    override fun result(): IntArray {
        val out = IntArray(n)
        for (i in 0 until n) {
            val c = count[i].coerceAtLeast(1)
            val r = (sumR[i] / c).toInt().coerceIn(0, 255)
            val g = (sumG[i] / c).toInt().coerceIn(0, 255)
            val b = (sumB[i] / c).toInt().coerceIn(0, 255)
            out[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
        return out
    }
}
