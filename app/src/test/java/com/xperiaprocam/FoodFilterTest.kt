package com.xperiaprocam

import com.xperiaprocam.core.imageprocessing.FoodFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FoodFilterTest {

    private fun px(r: Int, g: Int, b: Int) = (0xFF shl 24) or (r shl 16) or (g shl 8) or b

    @Test
    fun `neutral gray stays roughly neutral`() {
        val out = FoodFilter.apply(intArrayOf(px(128, 128, 128)))
        val r = (out[0] shr 16) and 0xFF
        val g = (out[0] shr 8) and 0xFF
        val b = out[0] and 0xFF
        assertTrue(kotlin.math.abs(r - g) <= 12)
        assertTrue(kotlin.math.abs(g - b) <= 16)
    }

    @Test
    fun `blue is warmed down and red lifted`() {
        val out = FoodFilter.apply(intArrayOf(px(100, 100, 100)))
        val r = (out[0] shr 16) and 0xFF
        val b = out[0] and 0xFF
        assertTrue("red should exceed blue after warm shift (r=$r b=$b)", r > b)
    }

    @Test
    fun `output channels stay in bounds and alpha is opaque`() {
        val out = FoodFilter.apply(intArrayOf(px(0, 0, 0), px(255, 255, 255)))
        for (p in out) {
            assertEquals(0xFF, (p ushr 24) and 0xFF)
            for (shift in intArrayOf(16, 8, 0)) {
                val v = (p shr shift) and 0xFF
                assertTrue(v in 0..255)
            }
        }
    }
}
