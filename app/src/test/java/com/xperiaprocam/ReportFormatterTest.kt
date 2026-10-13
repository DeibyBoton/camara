package com.xperiaprocam

import com.xperiaprocam.domain.model.CameraInfo
import com.xperiaprocam.domain.model.DeviceInfo
import com.xperiaprocam.domain.model.DiagnosticsReport
import com.xperiaprocam.domain.model.ReportFormatter
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportFormatterTest {

    private val report = DiagnosticsReport(
        device = DeviceInfo("SO-51C", "Sony", "15", 35, "fp"),
        cameras = listOf(
            CameraInfo(
                id = "0",
                facing = "trasera",
                hardwareLevel = "FULL",
                supportsRaw = true,
                supportsManualSensor = true,
                supportsBurst = false,
                isoRange = "50-6400",
                exposureRange = "1000-1000000000 ns",
                exposureCompensationRange = "-5..5 (paso 1/2)",
                jpegResolutions = listOf("4080x3060"),
                rawResolutions = listOf("4080x3060"),
                sonyVendorTags = listOf("com.sonymobile.some_tag"),
                hasFlash = true,
                stabilizationModes = emptyList(),
            ),
            CameraInfo(
                id = "1",
                facing = "frontal",
                hardwareLevel = "LIMITED",
                supportsRaw = false,
                supportsManualSensor = false,
                supportsBurst = false,
                isoRange = null,
                exposureRange = null,
                exposureCompensationRange = null,
                jpegResolutions = emptyList(),
                rawResolutions = emptyList(),
                sonyVendorTags = emptyList(),
                hasFlash = false,
                stabilizationModes = emptyList(),
            ),
        ),
        camera2ExtensionsAvailable = false,
    )

    @Test
    fun `text report shows No disponible for missing capabilities`() {
        val text = ReportFormatter.toText(report)
        assertTrue(text.contains("RAW: Sí"))
        assertTrue(text.contains("Burst: No"))
        assertTrue(text.contains("Rango ISO: No disponible"))
        assertTrue(text.contains("Vendor tags Sony: com.sonymobile.some_tag"))
        assertTrue(text.contains("Vendor tags Sony: ninguno"))
        assertFalse(text.contains("fp")) // fingerprint excluded from user-facing text
    }

    @Test
    fun `json report is parseable structure with booleans`() {
        val json = ReportFormatter.toJson(report)
        assertTrue(json.startsWith("{") && json.endsWith("}"))
        assertTrue(json.contains("\"raw\":true"))
        assertTrue(json.contains("\"raw\":false"))
        assertTrue(json.contains("\"sonyVendorTags\":[\"com.sonymobile.some_tag\"]"))
        assertFalse(json.contains("fp"))
    }
}
