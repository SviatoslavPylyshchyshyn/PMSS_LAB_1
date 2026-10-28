package com.example.intrusiondetector.helpers

import android.util.Log
import com.example.intrusiondetector.monitor.IntrusionState
import com.example.intrusiondetector.monitor.MonitorState

/**
 * Логічний блок прийняття рішень (DecisionEngine):
 * - Фіксує повертання пристрою та удари по столу
 * - При зафіксованому ударі/повороті (ALARM) сирена та спалах фіксуються в увімкненому стані
 *   і очікують на ручне вимкнення користувачем ("ЗУПИНИТИ ВСЕ").
 * - При стані попередження (WARNING) вібрація повертається до стану спокою через 15 секунд відсутності стимулу.
 */
class DecisionEngine(private val actuator: SmartActuator) {

    private val tag = "DecisionEngine"

    var isEnabled: Boolean = true

    private var lastWarningActiveTimestamp: Long = 0L

    /**
     * Аналізує координати сенсора, поворот пристрою та удари.
     */
    fun processSensorState(state: MonitorState) {
        if (!isEnabled) return

        val now = System.currentTimeMillis()

        when (state.intrusionState) {
            IntrusionState.ALARM -> {
                if (!actuator.isSirenPlaying) {
                    Log.d(tag, "DecisionEngine RULE MATCHED: [ALARM] delta = ${state.accelerationDelta} -> LATCH ALARM ON (Manual reset required)")
                    actuator.triggerAlarm()
                }
            }
            IntrusionState.WARNING -> {
                lastWarningActiveTimestamp = now
                if (!actuator.isVibrating && !actuator.isSirenPlaying) {
                    Log.d(tag, "DecisionEngine RULE MATCHED: [WARNING] delta = ${state.accelerationDelta} -> TRIGGER WARNING VIBRATION")
                    actuator.toggleVibration(true)
                }
            }
            IntrusionState.SAFE -> {
                // сирена та спалах при ALARM чекають ТІЛЬКИ ручного вимкнення ("ЗУПИНИТИ ВСЕ").
                // для стану WARNING (попередження): якщо минуло 15 секунд відсутності стимулу, вимикаємо вібрацію.
                if (actuator.isVibrating && !actuator.isSirenPlaying) {
                    if (now - lastWarningActiveTimestamp >= 15000L) { // 15 секунд відсутності стимулу
                        Log.d(tag, "DecisionEngine: 15 seconds of no stimulus -> WARNING auto-reset to SAFE")
                        actuator.toggleVibration(false)
                    }
                }
            }
        }
    }
}
