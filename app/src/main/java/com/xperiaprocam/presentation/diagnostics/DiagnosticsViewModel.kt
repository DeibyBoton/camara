package com.xperiaprocam.presentation.diagnostics

import androidx.lifecycle.ViewModel
import com.xperiaprocam.data.diagnostics.CameraDiagnosticsRepository
import com.xperiaprocam.data.diagnostics.ReportExporter
import com.xperiaprocam.domain.model.DiagnosticsReport
import com.xperiaprocam.domain.model.ReportFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    private val diagnostics: CameraDiagnosticsRepository,
    private val exporter: ReportExporter,
) : ViewModel() {

    val report: DiagnosticsReport by lazy { diagnostics.buildReport() }

    val reportText: String by lazy { ReportFormatter.toText(report) }

    fun export(): Boolean =
        exporter.exportJson(ReportFormatter.toJson(report)) != null &&
            exporter.exportText(reportText) != null
}
