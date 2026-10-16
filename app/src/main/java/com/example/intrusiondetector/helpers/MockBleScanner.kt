package com.example.intrusiondetector.helpers

import com.example.intrusiondetector.ble.DiscoveredDevice
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Імітатор BLE-сканера (MockBleScanner).
 * Через 2 секунди "знаходить" віртуальний IoT-пристрій Смарт-Хабу.
 */
class MockBleScanner {

    fun scanDevices(): Flow<Pair<Boolean, List<DiscoveredDevice>>> = flow {
        // початок сканування
        emit(Pair(true, emptyList()))

        // симуляція затримки пошуку пристроїв у 2 секунди
        delay(2000)

        // знайдені віртуальні IoT пристрої
        val virtualDevices = listOf(
            DiscoveredDevice(
                name = "IntrusionSmartHub-v1",
                address = "AA:BB:CC:11:22:33",
                rssi = -58
            ),
            DiscoveredDevice(
                name = "DoorSensorRelay-BLE",
                address = "DE:AD:BE:EF:00:01",
                rssi = -74
            )
        )

        // завершення сканування та повернення результатів
        emit(Pair(false, virtualDevices))
    }
}
