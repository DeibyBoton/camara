package com.xperiaprocam.core.imageprocessing

/**
 * Pure-Kotlin per-pixel stacker: averages aligned frames. Pixels outside the
 * shifted frame bounds are skipped so no dark borders appear.
 */
class FrameMerger(
    private val width: Int,
    private val height: Int,
    private val ghostThreshold: Int = 96,   // suma |dR|+|dG|+|dB|; ajusta según el ruido en poca luz
) : PixelMerger {
    private val n = width * height
    private val sumR = IntArray(n)
    private val sumG = IntArray(n)
    private val sumB = IntArray(n)
    private val count = ByteArray(n)          // válido hasta 127 frames
    private var ref: IntArray? = null
    private var frames = 0

    override fun add(pixels: IntArray, dx: Int, dy: Int) {
        require(pixels.size == n) { "frame size mismatch" }
        if (ref == null) ref = pixels
        val r = ref!!
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
                val q = r[i]
                val d = kotlin.math.abs(((p shr 16) and 0xFF) - ((q shr 16) and 0xFF)) +
                        kotlin.math.abs(((p shr 8) and 0xFF) - ((q shr 8) and 0xFF)) +
                        kotlin.math.abs((p and 0xFF) - (q and 0xFF))
                if (frames > 1 && d > ghostThreshold) continue
                sumR[i] += (p shr 16) and 0xFF
                sumG[i] += (p shr 8) and 0xFF
                sumB[i] += p and 0xFF
                count[i]++
            }
        }
    }

    fun frameCount(): Int = frames

    override fun result(): IntArray {
        val out = IntArray(n)
        for (i in 0 until n) {
            val c = count[i].toInt().coerceAtLeast(1)
            val r = (sumR[i] / c).coerceIn(0, 255)
            val g = (sumG[i] / c).coerceIn(0, 255)
            val b = (sumB[i] / c).coerceIn(0, 255)
            out[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
        return out
    }
}
