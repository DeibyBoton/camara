package com.xperiaprocam

import com.xperiaprocam.core.imageprocessing.FrameAligner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FrameAlignerTest {

    /** Builds an ARGB frame from a luminance function. */
    private fun frame(w: Int, h: Int, f: (Int, Int) -> Int): IntArray =
        IntArray(w * h) { i ->
            val x = i % w
            val y = i / w
            val v = f(x, y).coerceIn(0, 255)
            (0xFF shl 24) or (v shl 16) or (v shl 8) or v
        }

    @Test
    fun `identical frames give zero shift`() {
        val w = 96
        val h = 64
        val f = frame(w, h) { x, y -> (x * 3 + y * 5) and 0xFF }
        val shift = FrameAligner(workingWidth = 96).findShift(f, f, w, h)
        assertEquals(0, shift.dx)
        assertEquals(0, shift.dy)
    }

    @Test
    fun `finds pure translation of textured pattern`() {
        val w = 96
        val h = 64
        val ref = frame(w, h) { x, y -> (x * 7 + y * 13) and 0xFF }
        // moved(x, y) = ref(x - 3, y + 2): content sits 3px right and 2px up in `moved`
        val moved = frame(w, h) { x, y ->
            val sx = x - 3
            val sy = y + 2
            if (sx in 0 until w && sy in 0 until h) (sx * 7 + sy * 13) and 0xFF else 0
        }
        val shift = FrameAligner(workingWidth = 96, searchRadius = 8).findShift(ref, moved, w, h)
        // moved(x + dx, y + dy) ≈ ref(x, y) → dx = 3, dy = -2
        assertEquals(3, shift.dx)
        assertEquals(-2, shift.dy)
    }

    @Test
    fun `shift magnitude is plausible for flat frames`() {
        val w = 64
        val h = 64
        val flat = frame(w, h) { _, _ -> 128 }
        val shift = FrameAligner().findShift(flat, flat, w, h)
        assertTrue(kotlin.math.abs(shift.dx) <= 5 && kotlin.math.abs(shift.dy) <= 5)
    }

    @Test
    fun flatNoisyImagesReturnZeroShift() {
        val w = 256; val h = 192; val rnd = java.util.Random(1)
        fun noisy() = IntArray(w * h) { val v = 100 + rnd.nextInt(5); (0xFF shl 24) or (v shl 16) or (v shl 8) or v }
        val img = noisy()
        val s = FrameAligner().findShift(img, img, w, h)
        assertEquals(0, s.dx); assertEquals(0, s.dy)
    }
}
