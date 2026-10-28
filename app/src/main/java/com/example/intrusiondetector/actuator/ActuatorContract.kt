package com.example.intrusiondetector.actuator

/**
 * State — стан актуаторів та мануального керування (ЛР 4).
 */
data class ActuatorState(
    val isFlashlightOn: Boolean = false,
    val isSirenPlaying: Boolean = false,
    val isVibrating: Boolean = false,
    val isDecisionEngineEnabled: Boolean = true,
    val isCloudControlActive: Boolean = true,
    val hasFlashFeature: Boolean = true,
    val virtualLampStatus: String = "NORMAL (OFF)",
    val virtualLampColorHex: Long = 0xFF4CAF50, // Зелена за замовчуванням
    val logs: List<String> = emptyList()
)

/**
 * Action — дії користувача на екрані мануального керування (Manual Control).
 */
sealed interface ActuatorAction {
    data object ToggleFlashlight : ActuatorAction
    data object ToggleSiren : ActuatorAction
    data object ToggleVibration : ActuatorAction
    data object TriggerFullAlarm : ActuatorAction
    data object StopAllActuators : ActuatorAction
    data object ToggleDecisionEngine : ActuatorAction
    data object ClearLogs : ActuatorAction
}

/**
 * Event — разові сповіщення від ViewModel (Toast).
 */
sealed interface ActuatorEvent {
    data class ShowMessage(val text: String) : ActuatorEvent
}
