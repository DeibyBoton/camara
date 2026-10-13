package com.xperiaprocam.core.imageprocessing

import kotlin.math.exp

/**
 * Pure-Kotlin HDR merge: exposure-fusion weights per pixel/frame favor
 * well-exposed midtones, so highlights come from the dark frame and shadows
 * from the bright one. Sigma controls tolerance.
 */
class HdrMerger(
    private val width: Int,
    private val height: Int,
    private val sigma: Float = 0.2f,
) : PixelMerger {

    private val n = width * height
    private val wR = FloatArray(n)
    private val wG = FloatArray(n)
    private val wB = FloatArray(n)
    private val sumW = FloatArray(n)
    private var frames = 0

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
                val r = (p shr 16) and 0xFF
                val g = (p shr 8) and 0xFF
                val b = p and 0xFF
                // Well-exposedness from luma: midtones weigh most.
                val luma = (r * 299 + g * 587 + b * 114) / 1000 / 255f
                val w = exp(-((luma - 0.5f) * (luma - 0.5f)) / (2 * sigma * sigma))
                wR[i] += w * r
                wG[i] += w * g
                wB[i] += w * b
                sumW[i] += w
            }
        }
    }

    fun frameCount(): Int = frames

    override fun result(): IntArray {
        val out = IntArray(n)
        for (i in 0 until n) {
            val w = sumW[i]
            if (w <= 0f) continue
            val r = (wR[i] / w).toInt().coerceIn(0, 255)
            val g = (wG[i] / w).toInt().coerceIn(0, 255)
            val b = (wB[i] / w).toInt().coerceIn(0, 255)
            out[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
        return out
    }
}
