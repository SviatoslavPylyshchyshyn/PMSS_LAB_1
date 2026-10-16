package com.example.intrusiondetector.ble

/**
 * Стан підключення до BLE-пристрою.
 */
enum class BleConnectionState {
    DISCONNECTED, // Відключено
    CONNECTING,   // Встановити з'єднання...
    CONNECTED     // Підключено
}

/**
 * Модель знайденого віртуального BLE пристрою.
 */
data class DiscoveredDevice(
    val name: String,
    val address: String,
    val rssi: Int = -62
)

/**
 * State — стан екрана керування Bluetooth.
 */
data class BleState(
    val isScanning: Boolean = false,
    val discoveredDevices: List<DiscoveredDevice> = emptyList(),
    val selectedDevice: DiscoveredDevice? = null,
    val connectionState: BleConnectionState = BleConnectionState.DISCONNECTED,
    val logs: List<String> = emptyList(),
    val commandInputText: String = "ARM_SYSTEM"
)

/**
 * Action — дії користувача на екрані BLE.
 */
sealed interface BleAction {
    data object StartScan : BleAction
    data class ConnectDevice(val device: DiscoveredDevice) : BleAction
    data object Disconnect : BleAction
    data class UpdateCommandText(val text: String) : BleAction
    data class SendCommand(val command: String) : BleAction
    data object ClearLogs : BleAction
}

/**
 * Event — разові сповіщення від ViewModel (Toast, Snackbar).
 */
sealed interface BleEvent {
    data class ShowMessage(val text: String) : BleEvent
}
