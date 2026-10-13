package com.xperiaprocam.data.diagnostics

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.graphics.ImageFormat
import android.os.Build
import android.util.Range
import com.xperiaprocam.domain.model.CameraInfo
import com.xperiaprocam.domain.model.DeviceInfo
import com.xperiaprocam.domain.model.DiagnosticsReport
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Detects real device/camera capabilities via Camera2. No assumptions, no root. */
@Singleton
class CameraDiagnosticsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun buildReport(): DiagnosticsReport {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameras = manager.cameraIdList.mapNotNull { id ->
            runCatching { manager.getCameraCharacteristics(id) }.getOrNull()
                ?.let { characteristicsOf(id, it) }
        }
        return DiagnosticsReport(
            device = DeviceInfo(
                model = Build.MODEL ?: "unknown",
                manufacturer = Build.MANUFACTURER ?: "unknown",
                androidVersion = Build.VERSION.RELEASE ?: "unknown",
                sdkInt = Build.VERSION.SDK_INT,
                buildFingerprint = Build.FINGERPRINT ?: "",
            ),
            cameras = cameras,
            camera2ExtensionsAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                runCatching {
                    manager.getCameraExtensionCharacteristics(
                        idOfFirstBackCamera(manager) ?: manager.cameraIdList.firstOrNull() ?: ""
                    )
                }.isSuccess,
        )
    }

    private fun idOfFirstBackCamera(manager: CameraManager): String? =
        manager.cameraIdList.firstOrNull { id ->
            runCatching {
                manager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
            }.getOrDefault(false)
        }

    private fun characteristicsOf(id: String, c: CameraCharacteristics): CameraInfo {
        val capabilities =
            c.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES) ?: intArrayOf()
        val hasCap = { cap: Int -> capabilities.contains(cap) }

        val isoRange: Range<Int>? = c.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
        val expRange: Range<Long>? = c.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
        val ecStep = c.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP)
        val ecRange: Range<Int>? = c.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE)

        val sonyTags = c.availableCaptureRequestKeys
            .map { it.name }
            .filter { it.contains("sony", ignoreCase = true) }
            .distinct()
            .sorted()

        return CameraInfo(
            id = id,
            facing = when (c.get(CameraCharacteristics.LENS_FACING)) {
                CameraCharacteristics.LENS_FACING_BACK -> "trasera"
                CameraCharacteristics.LENS_FACING_FRONT -> "frontal"
                else -> "externa"
            },
            hardwareLevel = when (c.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)) {
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "LEVEL_3"
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "FULL"
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "LIMITED"
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> "LEGACY"
                else -> "No disponible"
            },
            supportsRaw = hasCap(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW),
            supportsManualSensor = hasCap(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR),
            supportsBurst = hasCap(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE),
            isoRange = isoRange?.let { "${it.lower}-${it.upper}" },
            exposureRange = expRange?.let { "${it.lower}-${it.upper} ns" },
            exposureCompensationRange = ecRange?.let { r ->
                val step = ecStep?.let { s -> " (paso ${s.numerator}/${s.denominator})" } ?: ""
                "${r.lower}..${r.upper}$step"
            },
            jpegResolutions = configurationMapSizes(c, ImageFormat.JPEG),
            rawResolutions = configurationMapSizes(c, ImageFormat.RAW_SENSOR),
            sonyVendorTags = sonyTags,
            hasFlash = c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true,
            stabilizationModes = (c.get(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES)
                ?: intArrayOf()).map { it.toString() },
        )
    }

    private fun configurationMapSizes(c: CameraCharacteristics, format: Int): List<String> =
        runCatching {
            c.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                ?.getOutputSizes(format)
                ?.take(4)
                ?.map { "${it.width}x${it.height}" }
                ?.sorted()
                ?: emptyList()
        }.getOrDefault(emptyList())
}
