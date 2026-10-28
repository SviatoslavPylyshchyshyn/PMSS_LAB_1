package com.example.intrusiondetector.sensor

import com.example.intrusiondetector.monitor.IntrusionState
import com.example.intrusiondetector.monitor.MonitorState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SensorDataTest {

    @Test
    fun magnitude_calculatedCorrectly() {
        val state = MonitorState(x = 3f, y = 4f, z = 0f)
        assertEquals(5.0f, state.magnitude, 0.001f)
    }

    @Test
    fun isLevel_returnsTrueWhenFlat() {
        val flatState = MonitorState(x = 0.1f, y = -0.2f, z = 9.81f)
        assertTrue(flatState.isLevel)
    }

    @Test
    fun isLevel_returnsFalseWhenTilted() {
        val tiltedState = MonitorState(x = 4.5f, y = 2.1f, z = 8.0f)
        assertFalse(tiltedState.isLevel)
    }

    @Test
    fun intrusionState_evaluatesSafeWhenResting() {
        val restingState = MonitorState(x = 0f, y = 0f, z = 9.81f)
        assertEquals(IntrusionState.SAFE, restingState.intrusionState)
    }

    @Test
    fun intrusionState_evaluatesWarningOnVibration() {
        val vibrationState = MonitorState(x = 0f, y = 0f, z = 11.0f)
        assertEquals(IntrusionState.WARNING, vibrationState.intrusionState)
    }

    @Test
    fun intrusionState_evaluatesAlarmOnImpact() {
        val impactState = MonitorState(x = 0f, y = 0f, z = 13.0f)
        assertEquals(IntrusionState.ALARM, impactState.intrusionState)
    }
}
