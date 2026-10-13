package com.xperiaprocam.data.diagnostics

import android.content.Context
import com.xperiaprocam.domain.model.ReportFormatter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Exports the diagnostics report to app-private storage (no personal data included). */
@Singleton
class ReportExporter @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun exportJson(json: String): File? = runCatching {
        File(context.filesDir, "diagnostics_report.json").apply { writeText(json) }
    }.getOrNull()

    fun exportText(text: String): File? = runCatching {
        File(context.filesDir, "diagnostics_report.txt").apply { writeText(text) }
    }.getOrNull()
}
