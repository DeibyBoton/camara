package com.xperiaprocam

import com.xperiaprocam.core.imageprocessing.PanoramaStitcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PanoramaStitcherTest {

    private fun frame(w: Int, h: Int, f: (Int, Int) -> Int): IntArray =
        IntArray(w * h) { i ->
            val x = i % w
            val y = i / w
            val v = f(x, y).coerceIn(0, 255)
            (0xFF shl 24) or (v shl 16) or (v shl 8) or v
        }

    @Test
    fun `single frame returns unchanged`() {
        val w = 40
        val h = 24
        val f = frame(w, h) { x, y -> x * 2 + y }
        val r = PanoramaStitcher().stitch(listOf(f), w, h)
        assertEquals(w, r.width)
        assertEquals(h, r.height)
        assertEquals(f.toList(), r.pixels.toList())
    }

    @Test
    fun `two frames overlapping yield a wider canvas`() {
        val w = 64
        val h = 32
        val base = frame(w, h) { x, y -> (x * 3 + y * 7) and 0xFF }
        // second frame shows the same scene shifted: right half of base + new content
        val second = frame(w, h) { x, y ->
            if (x < w / 2) ((x + w / 2) * 3 + y * 7) and 0xFF else 200 - (y and 0x1F)
        }
        val r = PanoramaStitcher(feather = 8).stitch(listOf(base, second), w, h)
        assertTrue("canvas should be wider than one frame (${r.width})", r.width > w)
        assertTrue(r.width <= 2 * w)
        assertEquals(h, r.height)
        // New material from the second frame (bright value ~200) must appear on the right end.
        val rightPixel = r.pixels[r.width - 1 + 0 * r.width]
        val g = (rightPixel shr 8) and 0xFF
        assertTrue("expected new content at right edge, got g=$g", g > 120)
    }

    @Test
    fun `identical frames collapse to same width`() {
        val w = 48
        val h = 32
        val f = frame(w, h) { x, y -> (x * 5 + y * 11) and 0xFF }
        val r = PanoramaStitcher().stitch(listOf(f.copyOf(), f.copyOf()), w, h)
        assertEquals(w, r.width)
    }
}
