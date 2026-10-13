package com.xperiaprocam.core.thermal

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThermalGuardian @Inject constructor(@ApplicationContext private val ctx: Context) {
    enum class State { NORMAL, WARM, HOT, CRITICAL }

    private val pm = ctx.getSystemService(PowerManager::class.java)
    private var lastCriticalAt = 0L

    fun batteryTempC(): Float? {
        val i = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return null
        val t = i.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
        return if (t == Int.MIN_VALUE) null else t / 10f
    }

    fun state(): State {
        val sys = pm?.currentThermalStatus ?: PowerManager.THERMAL_STATUS_NONE
        val temp = batteryTempC() ?: 0f
        val now = android.os.SystemClock.elapsedRealtime()
        val s = evaluateState(sys, temp)
        if (s == State.CRITICAL) lastCriticalAt = now
        return if (s != State.CRITICAL && now - lastCriticalAt < 60_000) State.HOT else s
    }

    companion object {
        fun evaluateState(sys: Int, tempC: Float): State = when {
            sys >= PowerManager.THERMAL_STATUS_SEVERE || tempC >= 44f -> State.CRITICAL
            sys >= PowerManager.THERMAL_STATUS_MODERATE || tempC >= 41f -> State.HOT
            sys >= PowerManager.THERMAL_STATUS_LIGHT || tempC >= 38f -> State.WARM
            else -> State.NORMAL
        }
    }
}
