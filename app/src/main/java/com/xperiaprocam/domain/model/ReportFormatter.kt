package com.xperiaprocam.domain.model

/** Formats a DiagnosticsReport as plain text or JSON, without personal data. */
object ReportFormatter {

    fun toText(report: DiagnosticsReport): String = buildString {
        appendLine("Modelo: ${report.device.model}")
        appendLine("Android: ${report.device.androidVersion}")
        appendLine("SDK: ${report.device.sdkInt}")
        appendLine("Fabricante: ${report.device.manufacturer}")
        appendLine("Camera2 extensions: ${yesNo(report.camera2ExtensionsAvailable)}")
        appendLine("Cámaras: ${report.cameras.size}")
        report.cameras.forEach { c ->
            appendLine("--- Cámara ${c.id} (${c.facing}) ---")
            appendLine("Camera2 level: ${c.hardwareLevel}")
            appendLine("RAW: ${yesNo(c.supportsRaw)}")
            appendLine("Manual sensor: ${yesNo(c.supportsManualSensor)}")
            appendLine("Burst: ${yesNo(c.supportsBurst)}")
            appendLine("Vendor tags Sony: ${c.sonyVendorTags.joinToString(", ").ifEmpty { "ninguno" }}")
            appendLine("Resoluciones RAW: ${c.rawResolutions.joinToString(", ").ifEmpty { "No disponible" }}")
            appendLine("Resoluciones JPEG: ${c.jpegResolutions.joinToString(", ").ifEmpty { "No disponible" }}")
            appendLine("Rango ISO: ${c.isoRange ?: "No disponible"}")
            appendLine("Rango exposición: ${c.exposureRange ?: "No disponible"}")
            appendLine("Rango compensación: ${c.exposureCompensationRange ?: "No disponible"}")
            appendLine("Flash: ${yesNo(c.hasFlash)}")
            appendLine("Estabilización: ${c.stabilizationModes.joinToString(", ").ifEmpty { "No disponible" }}")
        }
    }

    fun toJson(report: DiagnosticsReport): String {
        val cams = report.cameras.joinToString(",\n") { c ->
            """
            {"id":"${c.id}","facing":"${c.facing}","hardwareLevel":"${c.hardwareLevel}",
            "raw":${c.supportsRaw},"manualSensor":${c.supportsManualSensor},"burst":${c.supportsBurst},
            "isoRange":"${c.isoRange ?: ""}","exposureRange":"${c.exposureRange ?: ""}",
            "jpegResolutions":${c.jpegResolutions.toJsonArray()},
            "rawResolutions":${c.rawResolutions.toJsonArray()},
            "sonyVendorTags":${c.sonyVendorTags.toJsonArray()},
            "flash":${c.hasFlash},
            "stabilization":${c.stabilizationModes.toJsonArray()}}""".trimIndent().replace("\n", "").trim()
        }
        return """
        {"device":{"model":"${report.device.model}","manufacturer":"${report.device.manufacturer}",
        "android":"${report.device.androidVersion}","sdk":${report.device.sdkInt}},
        "camera2Extensions":${report.camera2ExtensionsAvailable},
        "cameras":[$cams]}""".trimIndent().replace("\n", "").trim()
    }

    private fun yesNo(b: Boolean) = if (b) "Sí" else "No"

    private fun List<String>.toJsonArray(): String =
        joinToString(",") { "\"$it\"" }.let { "[$it]" }
}
