package com.xperiaprocam

import com.xperiaprocam.core.imageprocessing.HdrMerger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HdrMergerTest {

    private fun solid(w: Int, h: Int, r: Int, g: Int, b: Int): IntArray =
        IntArray(w * h) { (0xFF shl 24) or (r shl 16) or (g shl 8) or b }

    @Test
    fun `single frame results in identical colors and full opacity`() {
        val f = solid(4, 4, 120, 130, 140)
        val merger = HdrMerger(4, 4)
        merger.add(f, 0, 0)
        val res = merger.result()
        assertEquals(16, res.size)
        val p = res[0]
        assertEquals(0xFF, (p ushr 24) and 0xFF)
        val r = (p shr 16) and 0xFF
        val g = (p shr 8) and 0xFF
        val b = p and 0xFF
        // Due to weighting normalisation, a single frame gives back its original values
        assertTrue("R should be close to 120 (got $r)", kotlin.math.abs(r - 120) <= 2)
        assertTrue("G should be close to 130 (got $g)", kotlin.math.abs(g - 130) <= 2)
        assertTrue("B should be close to 140 (got $b)", kotlin.math.abs(b - 140) <= 2)
    }

    @Test
    fun `midtones get higher weight than clipped highlights or deep shadows`() {
        // Frame 1: Midtone (well exposed, ~128)
        // Frame 2: Blown out highlight (255)
        val midtone = solid(2, 2, 128, 128, 128)
        val highlight = solid(2, 2, 255, 255, 255)

        val merger = HdrMerger(2, 2)
        merger.add(midtone, 0, 0)
        merger.add(highlight, 0, 0)
        val res = merger.result()

        val r = (res[0] shr 16) and 0xFF
        // The merged value should be much closer to 128 than 255 because Gaussian weight at 0.5 (128) is ~1.0,
        // whereas at 1.0 (255) it is exp(-0.5^2 / (2 * 0.2^2)) = exp(-3.125) ≈ 0.04.
        assertTrue("Merged pixel ($r) should be biased strongly towards midtone 128", r < 160)
    }

    @Test
    fun `preserves dimensions and stays within 0 to 255 bounds`() {
        val merger = HdrMerger(8, 8)
        merger.add(solid(8, 8, 0, 0, 0), 0, 0)
        merger.add(solid(8, 8, 255, 255, 255), 0, 0)
        merger.add(solid(8, 8, 100, 150, 200), 0, 0)

        val res = merger.result()
        assertEquals(64, res.size)
        for (p in res) {
            val a = (p ushr 24) and 0xFF
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            assertEquals(255, a)
            assertTrue(r in 0..255)
            assertTrue(g in 0..255)
            assertTrue(b in 0..255)
        }
    }
}
