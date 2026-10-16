package com.example.intrusiondetector.helpers

import com.example.intrusiondetector.ble.BleConnectionState
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

/**
 * Профіль BLE пристрою: UUID для сервісу та характеристик.
 */
object BleGattProfile {
    /** Основний сервіс розумного хабу вторгнень */
    val SERVICE_UUID: UUID = UUID.fromString("0000180F-0000-1000-8000-00805f9b34fb")

    /** Характеристика для зчитування статусу пристрою */
    val STATUS_CHAR_UUID: UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")

    /** Характеристика для відправки команд на пристрій */
    val COMMAND_CHAR_UUID: UUID = UUID.fromString("12345678-1234-1234-1234-123456789def")
}

/**
 * Інтерфейс для керування BLE-з'єднанням.
 */
interface IBleConnector {
    val connectionState: StateFlow<BleConnectionState>
    val logs: StateFlow<List<String>>

    suspend fun connect(deviceAddress: String): Boolean
    fun disconnect()
    suspend fun sendData(data: String): Boolean
    fun clearLogs()
}
