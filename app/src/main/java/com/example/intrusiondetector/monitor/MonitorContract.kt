package com.example.intrusiondetector.monitor

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Перелік станів безпеки пристрою на основі порогових значень (Threshold).
 */
enum class IntrusionState {
    SAFE,      // спокій: пристрій нерухомий
    WARNING,   // попередження: виявлено помірну вібрацію
    ALARM      // тривога: сильний удар або переміщення
}

/**
 * State — усе, що відображається на екрані. Тільки val, значення за замовчуванням обов'язкові.
 */
data class MonitorState(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f,
    val isRunning: Boolean = false,
    val isFilterEnabled: Boolean = true,
    val sensorName: String = "Визначення..."
) {
    companion object {
        const val WARNING_THRESHOLD_M_S2 = 1.0f
        const val ALARM_THRESHOLD_M_S2 = 2.5f
        const val GRAVITY_EARTH = 9.81f
    }

    val magnitude: Float
        get() = sqrt((x * x) + (y * y) + (z * z))

    val accelerationDelta: Float
        get() = abs(magnitude - GRAVITY_EARTH)

    val intrusionState: IntrusionState
        get() = when {
            accelerationDelta >= ALARM_THRESHOLD_M_S2 -> IntrusionState.ALARM
            accelerationDelta >= WARNING_THRESHOLD_M_S2 -> IntrusionState.WARNING
            else -> IntrusionState.SAFE
        }

    val isLevel: Boolean
        get() = abs(x) < 0.8f && abs(y) < 0.8f
}

/**
 * Action — усі дії користувача та системні події Activity на цьому екрані.
 */
sealed interface MonitorAction {
    data object Start : MonitorAction
    data object Stop : MonitorAction
    data object ToggleFilter : MonitorAction
}

/**
 * Event — разові повідомлення від ViewModel до екрана.
 */
sealed interface MonitorEvent {
    data class ShowMessage(val text: String) : MonitorEvent
}
