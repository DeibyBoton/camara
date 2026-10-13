package com.xperiaprocam

import com.xperiaprocam.core.imageprocessing.LookFilter
import com.xperiaprocam.domain.model.CaptureLook
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LookFilterTest {

    private fun px(r: Int, g: Int, b: Int) = (0xFF shl 24) or (r shl 16) or (g shl 8) or b

    @Test
    fun `NATURAL returns identity clone`() {
        val src = intArrayOf(px(10, 20, 30), px(200, 150, 100))
        val res = LookFilter.apply(CaptureLook.NATURAL, src)
        assertEquals(src.toList(), res.toList())
    }

    @Test
    fun `MONO converts color to grayscale`() {
        val src = intArrayOf(px(255, 0, 0), px(0, 255, 0), px(0, 0, 255))
        val res = LookFilter.apply(CaptureLook.MONO, src)
        for (p in res) {
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            // Grayscale saturation=0 means r == g == b
            assertEquals("R and G should be equal in monochrome", r, g)
            assertEquals("G and B should be equal in monochrome", g, b)
        }
    }

    @Test
    fun `WARM shifts reds higher and blues lower`() {
        val src = intArrayOf(px(100, 100, 100))
        val res = LookFilter.apply(CaptureLook.WARM, src)
        val r = (res[0] shr 16) and 0xFF
        val b = res[0] and 0xFF
        assertTrue("Red ($r) should be warmer/higher than Blue ($b)", r > b)
    }

    @Test
    fun `all filters preserve opacity and clamped channel values`() {
        val src = intArrayOf(px(0, 0, 0), px(255, 255, 255), px(128, 64, 192))
        for (look in CaptureLook.values()) {
            val res = LookFilter.apply(look, src)
            assertEquals(src.size, res.size)
            for (p in res) {
                assertEquals(0xFF, (p ushr 24) and 0xFF)
                val r = (p shr 16) and 0xFF
                val g = (p shr 8) and 0xFF
                val b = p and 0xFF
                assertTrue(r in 0..255)
                assertTrue(g in 0..255)
                assertTrue(b in 0..255)
            }
        }
    }
}
