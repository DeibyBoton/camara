package com.xperiaprocam.data.camera

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Range
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves a safe manual exposure (star mode) clamped to the ranges the HAL
 * actually reports. Never sends out-of-range values; returns null when the
 * camera lacks MANUAL_SENSOR support (star mode then degrades to AE).
 */
@Singleton
class ManualExposureResolver @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    data class ManualExposure(val iso: Int, val exposureNs: Long, val cameraId: String)

    fun resolve(facingBack: Boolean): ManualExposure? {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val id = manager.cameraIdList.firstOrNull { camId ->
            runCatching {
                val c = manager.getCameraCharacteristics(camId)
                val facing = c.get(CameraCharacteristics.LENS_FACING)
                val back = facing == CameraCharacteristics.LENS_FACING_BACK
                val front = facing == CameraCharacteristics.LENS_FACING_FRONT
                val hasManual = (c.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
                    ?: intArrayOf())
                    .contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR)
                hasManual && (back == facingBack || (front && !facingBack))
            }.getOrDefault(false)
        } ?: return null

        val c = manager.getCameraCharacteristics(id)
        val isoRange: Range<Int> = c.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
            ?: return null
        val expRange: Range<Long> = c.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
            ?: return null

        val desiredIso = 3200
        val desiredExposureNs = 2_000_000_000L // 2 s: enough light, short enough to limit trails
        return ManualExposure(
            iso = desiredIso.coerceIn(isoRange.lower, isoRange.upper),
            exposureNs = desiredExposureNs.coerceIn(expRange.lower, expRange.upper),
            cameraId = id,
        )
    }
}
