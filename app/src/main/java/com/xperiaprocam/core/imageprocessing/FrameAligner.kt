package com.xperiaprocam.core.imageprocessing

import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Pure-Kotlin translation-only frame aligner for handheld bursts and panoramas.
 *
 * Works on ARGB pixel arrays: downsamples to a small luma plane and searches
 * the integer shift that minimizes SAD against the reference frame. Horizontal
 * and vertical search radii are independent (panoramas need wide dx, small dy).
 */
class FrameAligner(
    private val workingWidth: Int = 256,
    private val searchRadius: Int = 20,
    private val searchDyRadius: Int = searchRadius,
) {
    data class Shift(val dx: Int, val dy: Int)

    /**
     * Returns the shift (dx, dy) in full-resolution pixels such that
     * frame(x + dx, y + dy) ≈ ref(x, y).
     */
    fun findShift(ref: IntArray, frame: IntArray, width: Int, height: Int): Shift {
        require(ref.size == width * height && frame.size == width * height)
        if (width <= workingWidth) {
            val refL = IntArray(ref.size) { luma(ref[it]) }
            val frameL = IntArray(frame.size) { luma(frame[it]) }
            return searchShift(refL, frameL, width, height, dxRadius = 4, dyRadius = 4, scale = 1f)
        }
        val scale = workingWidth.toFloat() / width
        val sw = workingWidth
        val sh = max(1, (height * scale).roundToInt())
        val refS = downsampleLuma(ref, width, height, sw, sh)
        val frameS = downsampleLuma(frame, width, height, sw, sh)
        val small = searchShift(refS, frameS, sw, sh, dxRadius = searchRadius, dyRadius = searchDyRadius, scale = scale)
        return Shift(
            (small.dx / scale).roundToInt(),
            (small.dy / scale).roundToInt(),
        )
    }

    private fun searchShift(
        ref: IntArray,
        frame: IntArray,
        width: Int,
        height: Int,
        dxRadius: Int,
        dyRadius: Int,
        scale: Float,
    ): Shift {
        var bestDx = 0
        var bestDy = 0
        var bestSad = Long.MAX_VALUE
        for (dy in -dyRadius..dyRadius) {
            for (dx in -dxRadius..dxRadius) {
                val sad = sad(ref, frame, width, height, dx, dy)
                if (sad < bestSad) {
                    bestSad = sad
                    bestDx = dx
                    bestDy = dy
                }
            }
        }
        return Shift(bestDx, bestDy)
    }

    private fun sad(ref: IntArray, frame: IntArray, w: Int, h: Int, dx: Int, dy: Int): Long {
        var sum = 0L
        var n = 0
        val y0 = max(0, -dy)
        val y1 = min(h, h - dy)
        val x0 = max(0, -dx)
        val x1 = min(w, w - dx)
        for (y in y0 until y1) {
            val rowRef = y * w
            val rowFrame = (y + dy) * w
            for (x in x0 until x1) {
                sum += kotlin.math.abs(ref[rowRef + x] - frame[rowFrame + x + dx])
                n++
            }
        }
        if (n < (w * h) / 2) return Long.MAX_VALUE
        return (sum * 1000L) / n
    }

    private fun downsampleLuma(src: IntArray, w: Int, h: Int, sw: Int, sh: Int): IntArray {
        val out = IntArray(sw * sh)
        for (sy in 0 until sh) {
            val y0 = sy * h / sh
            val y1 = max(y0 + 1, (sy + 1) * h / sh)
            for (sx in 0 until sw) {
                val x0 = sx * w / sw
                val x1 = max(x0 + 1, (sx + 1) * w / sw)
                var acc = 0L
                var n = 0
                for (y in y0 until y1) {
                    val row = y * w
                    for (x in x0 until x1) {
                        val p = src[row + x]
                        acc += luma(p)
                        n++
                    }
                }
                out[sy * sw + sx] = (acc / n).toInt()
            }
        }
        return out
    }

    private fun luma(argb: Int): Int {
        val r = (argb shr 16) and 0xFF
        val g = (argb shr 8) and 0xFF
        val b = argb and 0xFF
        return (r * 299 + g * 587 + b * 114) / 1000
    }

    private fun min(a: Int, b: Int) = if (a < b) a else b
    private fun max(a: Int, b: Int) = if (a > b) a else b
}
