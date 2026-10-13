package com.xperiaprocam.data.status

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.math.sqrt
import kotlin.coroutines.resume

/**
 * Lightweight tripod/stability detection for astrophotography: samples the
 * accelerometer for [sampleMs] and reports whether the device is steady enough.
 */
class StabilityDetector(context: Context) {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    /** @return true when movement variance is below the handheld threshold. */
    suspend fun isStable(sampleMs: Long = 1200): Boolean =
        suspendCancellableCoroutine { cont ->
            val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            if (sensor == null) {
                if (cont.isActive) cont.resume(true)
                return@suspendCancellableCoroutine
            }
            val samples = mutableListOf<Float>()
            val start = System.currentTimeMillis()
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    val (x, y, z) = event.values
                    samples.add(sqrt(x * x + y * y + z * z))
                    if (System.currentTimeMillis() - start >= sampleMs) {
                        sensorManager.unregisterListener(this)
                        if (cont.isActive) {
                            cont.resume(std(samples) < STABLE_STD)
                        }
                    }
                }

                override fun onAccuracyChanged(s: Sensor?, accuracy: Int) {}
            }
            sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
            cont.invokeOnCancellation { sensorManager.unregisterListener(listener) }
        }

    private fun std(values: List<Float>): Float {
        if (values.size < 2) return Float.MAX_VALUE
        val mean = values.average().toFloat()
        val variance = values.sumOf { ((it - mean) * (it - mean)).toDouble() } / values.size
        return sqrt(variance).toFloat()
    }

    companion object {
        const val STABLE_STD = 0.08f
    }
}
