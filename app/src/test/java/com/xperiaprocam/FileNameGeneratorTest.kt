package com.xperiaprocam

import com.xperiaprocam.domain.model.FileNameGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class FileNameGeneratorTest {

    private val time = LocalDateTime.of(2026, 10, 12, 18, 30, 5)

    @Test
    fun `jpeg name matches spec pattern`() {
        assertEquals("XPC_20261012_183005.jpg", FileNameGenerator.jpeg(time))
    }

    @Test
    fun `dng name matches spec pattern`() {
        assertEquals("XPC_20261012_183005.dng", FileNameGenerator.dng(time))
    }

    @Test
    fun `suffix name is inserted before extension`() {
        assertEquals("XPC_20261012_183005_HDR.jpg", FileNameGenerator.withSuffix("HDR", time))
        assertEquals("XPC_20261012_183005_NIGHT.jpg", FileNameGenerator.withSuffix("NIGHT", time))
    }

    @Test
    fun `names are unique per second`() {
        val a = FileNameGenerator.jpeg(time)
        val b = FileNameGenerator.jpeg(time.plusSeconds(1))
        assertTrue(a != b)
    }
}
