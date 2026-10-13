package com.xperiaprocam.domain.model

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** Generates unique, human-readable capture file names: XPC_YYYYMMDD_HHMMSS(.jpg) */
object FileNameGenerator {
    private val FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")

    fun jpeg(time: LocalDateTime = LocalDateTime.now()): String = base(time) + ".jpg"

    fun dng(time: LocalDateTime = LocalDateTime.now()): String = base(time) + ".dng"

    fun withSuffix(suffix: String, time: LocalDateTime = LocalDateTime.now()): String =
        base(time) + "_" + suffix + ".jpg"

    private fun base(time: LocalDateTime): String = "XPC_" + FORMAT.format(time)
}
