package com.xperiaprocam

import com.xperiaprocam.core.imageprocessing.FrameMerger
import org.junit.Assert.assertEquals
import org.junit.Test

class FrameMergerTest {

    private fun solid(w: Int, h: Int, r: Int, g: Int, b: Int): IntArray =
        IntArray(w * h) { (0xFF shl 24) or (r shl 16) or (g shl 8) or b }

    @Test
    fun `identical frames average to themselves`() {
        val f = solid(8, 8, 100, 150, 200)
        val m = FrameMerger(8, 8)
        m.add(f, 0, 0)
        m.add(f, 0, 0)
        m.add(f, 0, 0)
        assertEquals(f.toList(), m.result().toList())
        assertEquals(3, m.frameCount())
    }

    @Test
    fun `two colors average channel-wise`() {
        val m = FrameMerger(4, 4)
        m.add(solid(4, 4, 0, 100, 200), 0, 0)
        m.add(solid(4, 4, 100, 100, 100), 0, 0)
        val px = m.result()
        val expected = (0xFF shl 24) or (50 shl 16) or (100 shl 8) or 150
        assertEquals(expected, px[0])
        assertEquals(expected, px.last())
    }

    @Test
    fun `noise averages toward the constant scene value`() {
        val w = 32
        val h = 32
        val m = FrameMerger(w, h)
        val rng = java.util.Random(42)
        repeat(8) { i ->
            val noisy = IntArray(w * h) { j ->
                val v = 100 + rng.nextInt(41) - 20
                (0xFF shl 24) or (v shl 16) or (v shl 8) or v
            }
            m.add(noisy, 0, 0)
        }
        // With 8 frames of ±20 uniform noise, the mean should be well within ±6 of 100.
        val px = m.result()
        val sampleR = (px[0] shr 16) and 0xFF
        org.junit.Assert.assertTrue(kotlin.math.abs(sampleR - 100) <= 6)
    }

    @Test
    fun `shifted frame contributes only inside bounds`() {
        val m = FrameMerger(4, 4)
        m.add(solid(4, 4, 0, 0, 0), 0, 0)          // reference: black
        m.add(solid(4, 4, 255, 255, 255), 2, 0)    // white, sampled at x+2
        val px = m.result()
        fun red(i: Int) = (px[i] shr 16) and 0xFF
        // frame(x+2) in-bounds for x=0..1 → those average (0+255)/2 = 127; x=2..3 see black only
        assertEquals(127, red(0))
        assertEquals(127, red(1))
        assertEquals(0, red(2))
        assertEquals(0, red(3))
        assertEquals(0, red(4 * 4 - 1))
    }
}
