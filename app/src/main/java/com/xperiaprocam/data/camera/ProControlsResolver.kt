package com.xperiaprocam.data.camera

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CameraManager
import android.util.Range
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reports the real, clamped ranges for Pro mode controls. All values come from
 * CameraCharacteristics; anything unsupported comes back null and the UI hides
 * the corresponding control (spec: never send out-of-range values).
 */
@Singleton
class ProControlsResolver @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    data class ProRanges(
        val isoRange: IntRange?,
        val exposureNsRange: LongRange?,
        val minFocusDistance: Float?,        // diopters; null = fixed focus
        val maxJpegSize: Pair<Int, Int>?,    // w to h, sensor max JPEG
    )

    fun resolve(facingBack: Boolean): ProRanges {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val id = pickCamera(manager, facingBack) ?: return ProRanges(null, null, null, null)
        val c = manager.getCameraCharacteristics(id)

        val iso: Range<Int>? = c.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
        val exp: Range<Long>? = c.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
        val focus: Float? = c.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)
        val maxJpeg = runCatching {
            c.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                ?.getOutputSizes(android.graphics.ImageFormat.JPEG)
                ?.maxByOrNull { it.width.toLong() * it.height }
                ?.let { it.width to it.height }
        }.getOrNull()

        return ProRanges(
            isoRange = iso?.let { it.lower..it.upper },
            exposureNsRange = exp?.let { it.lower..it.upper },
            minFocusDistance = focus?.takeIf { it > 0f },
            maxJpegSize = maxJpeg,
        )
    }

    private fun pickCamera(manager: CameraManager, facingBack: Boolean): String? {
        val target = if (facingBack) CameraCharacteristics.LENS_FACING_BACK
        else CameraCharacteristics.LENS_FACING_FRONT
        return manager.cameraIdList.firstOrNull { id ->
            runCatching {
                manager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.LENS_FACING) == target
            }.getOrDefault(false)
        }
    }
}
