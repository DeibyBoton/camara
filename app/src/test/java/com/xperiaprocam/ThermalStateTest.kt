package com.xperiaprocam

import android.os.PowerManager
import com.xperiaprocam.core.thermal.ThermalGuardian
import org.junit.Assert.assertEquals
import org.junit.Test

class ThermalStateTest {

    @Test
    fun `evaluates states by thermal thresholds correctly`() {
        assertEquals(
            ThermalGuardian.State.NORMAL,
            ThermalGuardian.evaluateState(PowerManager.THERMAL_STATUS_NONE, 30f)
        )
        assertEquals(
            ThermalGuardian.State.WARM,
            ThermalGuardian.evaluateState(PowerManager.THERMAL_STATUS_LIGHT, 30f)
        )
        assertEquals(
            ThermalGuardian.State.WARM,
            ThermalGuardian.evaluateState(PowerManager.THERMAL_STATUS_NONE, 39f)
        )
        assertEquals(
            ThermalGuardian.State.HOT,
            ThermalGuardian.evaluateState(PowerManager.THERMAL_STATUS_MODERATE, 30f)
        )
        assertEquals(
            ThermalGuardian.State.HOT,
            ThermalGuardian.evaluateState(PowerManager.THERMAL_STATUS_NONE, 42f)
        )
        assertEquals(
            ThermalGuardian.State.CRITICAL,
            ThermalGuardian.evaluateState(PowerManager.THERMAL_STATUS_SEVERE, 30f)
        )
        assertEquals(
            ThermalGuardian.State.CRITICAL,
            ThermalGuardian.evaluateState(PowerManager.THERMAL_STATUS_NONE, 45f)
        )
    }
}
