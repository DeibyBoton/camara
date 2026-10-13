package com.xperiaprocam.core.imageprocessing

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Feeds JPEG frames into the aligner + stacker and produces a merged Bitmap.
 * Memory-safe: only one scaled Bitmap and one reference pixel array are held
 * at a time; per-frame bitmaps are recycled by the caller after [add].
 */
class NightStackPipeline(
    private val maxSide: Int = 2048,
    private val mergerFactory: (Int, Int) -> PixelMerger = { w, h -> FrameMerger(w, h) },
) {

    private val aligner = FrameAligner()
    private var merger: PixelMerger? = null
    private var refPixels: IntArray? = null
    private var width = 0
    private var height = 0
    private var received = 0

    /** Decodes a JPEG, applies the capture rotation, downscales to [maxSide] and stacks it. */
    fun addJpeg(jpegBytes: ByteArray, rotationDegrees: Int = 0) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return

        // Decode power-of-two downscaled, then exact-scale to maxSide.
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size, opts) ?: return

        val longSide = max(decoded.width, decoded.height)
        val scale = if (longSide > maxSide) maxSide.toFloat() / longSide else 1f
        val rotated = rotateIfNeeded(decoded, rotationDegrees)
        val working = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                rotated,
                (rotated.width * scale).roundToInt(),
                (rotated.height * scale).roundToInt(),
                true,
            )
        } else rotated

        val w = working.width
        val h = working.height
        val px = IntArray(w * h)
        working.getPixels(px, 0, w, 0, 0, w, h)

        synchronized(this) {
            if (merger == null) {
                width = w
                height = h
                refPixels = px
                merger = mergerFactory(w, h).also { it.add(px, 0, 0) }
            } else {
                val (dx, dy) = aligner.findShift(refPixels!!, px, w, h)
                merger!!.add(px, dx, dy)
            }
            received++
        }

        if (working !== rotated) working.recycle()
        if (rotated !== decoded) rotated.recycle()
        decoded.recycle()
    }

    private fun rotateIfNeeded(src: Bitmap, degrees: Int): Bitmap =
        if (degrees == 0) src else runCatching {
            val m = Matrix().apply { postRotate(degrees.toFloat()) }
            Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
        }.getOrDefault(src)

    fun frameCount(): Int = synchronized(this) { received }

    /** @return merged bitmap or null if no frame was successfully added. */
    fun result(): Bitmap? = synchronized(this) {
        val m = merger ?: return null
        val pixels = m.result()
        Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }
}
