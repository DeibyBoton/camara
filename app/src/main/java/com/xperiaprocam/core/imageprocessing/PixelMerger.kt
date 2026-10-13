package com.xperiaprocam.core.imageprocessing

/** Common interface for multi-frame stackers (average, HDR weighted merge). */
interface PixelMerger {
    fun add(pixels: IntArray, dx: Int, dy: Int)
    fun result(): IntArray
}
