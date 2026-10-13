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
                val c = count[i].toInt()
                val (qr, qg, qb) = if (c > 0) {
                    Triple(sumR[i] / c, sumG[i] / c, sumB[i] / c)
                } else {
                    val q0 = r[i]
                    Triple((q0 shr 16) and 0xFF, (q0 shr 8) and 0xFF, q0 and 0xFF)
                }
                val pr = (p shr 16) and 0xFF
                val pg = (p shr 8) and 0xFF
                val pb = p and 0xFF
                val d = kotlin.math.abs(pr - qr) +
                        kotlin.math.abs(pg - qg) +
                        kotlin.math.abs(pb - qb)
                if (frames > 1 && d > ghostThreshold) continue
                sumR[i] += pr
                sumG[i] += pg
                sumB[i] += pb
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
