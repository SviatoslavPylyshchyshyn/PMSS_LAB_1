package com.example.intrusiondetector.helpers

import android.util.Log
import com.example.intrusiondetector.ble.BleConnectionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Реалізація Bluetooth Interface Layer (MockBleConnector).
 * Імітує роботу з BLE GATT сервісом та характеристиками.
 */
class MockBleConnector : IBleConnector {

    private val TAG = "MockBleConnector"

    private val _connectionState = MutableStateFlow(BleConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    override val logs: StateFlow<List<String>> = _logs.asStateFlow()

    private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    override suspend fun connect(deviceAddress: String): Boolean {
        addLog("-> Запит на підключення до BLE [$deviceAddress]...")
        Log.d(TAG, "Connecting to GATT device at $deviceAddress...")

        _connectionState.value = BleConnectionState.CONNECTING
        delay(1500) // симуляція GATT підключення

        _connectionState.value = BleConnectionState.CONNECTED
        addLog("Успішно підключено до BLE GATT [$deviceAddress]")
        addLog("GATT Service UUID: ${BleGattProfile.SERVICE_UUID}")
        addLog("Status Char UUID:  ${BleGattProfile.STATUS_CHAR_UUID}")
        addLog("Command Char UUID: ${BleGattProfile.COMMAND_CHAR_UUID}")

        Log.d(TAG, "Connected to GATT Service [${BleGattProfile.SERVICE_UUID}]")
        return true
    }

    override fun disconnect() {
        _connectionState.value = BleConnectionState.DISCONNECTED
        addLog("<- Відключено від BLE GATT пристрою")
        Log.d(TAG, "Disconnected from GATT")
    }

    override suspend fun sendData(data: String): Boolean {
        if (_connectionState.value != BleConnectionState.CONNECTED) {
            addLog("Помилка: немає активного з'єднання з BLE пристроєм")
            Log.w(TAG, "Send data failed: Not connected")
            return false
        }

        addLog("[TX -> GATT Command Char]: $data")
        Log.d(TAG, "GATT Write Characteristic [${BleGattProfile.COMMAND_CHAR_UUID}]: $data")

        delay(300) // симуляція відповіді від пристрою
        addLog("[RX <- GATT Status Char]: ACK_OK ($data received)")
        Log.d(TAG, "GATT Read Response ACK_OK for command: $data")
        return true
    }

    override fun clearLogs() {
        _logs.value = emptyList()
    }

    private fun addLog(message: String) {
        val timestamp = dateFormat.format(Date())
        val logEntry = "[$timestamp] $message"
        _logs.update { currentLogs -> currentLogs + logEntry }
    }
}
