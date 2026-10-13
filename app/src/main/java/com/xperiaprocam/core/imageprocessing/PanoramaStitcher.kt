package com.xperiaprocam.core.imageprocessing

/**
 * Pure-Kotlin translation-based panorama stitcher for horizontal sweeps.
 * Consecutive frames are aligned with a wide horizontal search and pasted onto
 * a shared canvas with a linear feather in the overlap zone. Good for distant
 * scenes; parallax in near objects will show seams (documented limitation).
 */
class PanoramaStitcher(
    private val aligner: FrameAligner = FrameAligner(
        workingWidth = 256,
        searchRadius = 100,
        searchDyRadius = 10,
    ),
    private val feather: Int = 32,
) {
    /**
     * @param frames same-sized ARGB pixel arrays, captured left→right.
     * @return the stitched (possibly wide) ARGB pixels plus final dimensions.
     */
    fun stitch(frames: List<IntArray>, w: Int, h: Int): Result {
        require(frames.isNotEmpty()) { "no frames" }
        require(frames.all { it.size == w * h }) { "frame size mismatch" }
        val canvasW = w * frames.size
        val canvas = IntArray(canvasW * h)
        var offsetX = 0
        var prev: IntArray = frames[0]
        paste(canvas, frames[0], w, h, 0, feather = 0)
        for (i in 1 until frames.size) {
            val shift = aligner.findShift(prev, frames[i], w, h)
            // frame[i](x+dx) ≈ prev(x) → frame[i] pixel X lands at offsetX + X − dx
            val newOffset = (offsetX - shift.dx).coerceIn(0, canvasW - w)
            paste(canvas, frames[i], w, h, newOffset, feather = feather)
            if (newOffset + w > offsetX + w) {
                prev = frames[i]
                offsetX = newOffset
            }
        }
        val usedW = (offsetX + w).coerceAtMost(canvasW)
        val outW = usedW.coerceAtLeast(w)
        return Result(cropHorizontal(canvas, canvasW, outW, h), outW, h)
    }

    private fun paste(canvas: IntArray, f: IntArray, w: Int, h: Int, offsetX: Int, feather: Int) {
        for (y in 0 until h) {
            for (x in 0 until w) {
                val cx = offsetX + x
                if (cx >= canvas.size / h) continue
                val dst = y * (canvas.size / h) + cx
                val src = y * w + x
                val old = canvas[dst]
                if (old ushr 24 == 0) {
                    canvas[dst] = f[src]
                } else if (feather > 0 && x < feather) {
                    val a = x.toFloat() / feather
                    canvas[dst] = blend(old, f[src], a)
                } else {
                    canvas[dst] = f[src]
                }
            }
        }
    }

    private fun blend(a: Int, b: Int, t: Float): Int {
        fun ch(x: Int, y: Int): Int = (x + (y - x) * t).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or
            (ch((a shr 16) and 0xFF, (b shr 16) and 0xFF) shl 16) or
            (ch((a shr 8) and 0xFF, (b shr 8) and 0xFF) shl 8) or
            ch(a and 0xFF, b and 0xFF)
    }

    private fun cropHorizontal(src: IntArray, srcW: Int, outW: Int, h: Int): IntArray {
        if (outW == srcW) return src
        val out = IntArray(outW * h)
        for (y in 0 until h) {
            System.arraycopy(src, y * srcW, out, y * outW, outW)
        }
        return out
    }

    data class Result(val pixels: IntArray, val width: Int, val height: Int)
}
