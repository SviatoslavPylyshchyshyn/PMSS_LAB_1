package com.example.intrusiondetector.repository

/**
 * DTO (Data Transfer Object) модель даних для збереження та відправки у хмару (Firebase JSON).
 */
data class SensorPayload(
    val sensorType: String = "ACCELEROMETER",
    val deviceId: String = "Smartphone-Sensor-01",
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f,
    val magnitude: Float = 0f,
    val intrusionState: String = "SAFE",
    val timestamp: Long = System.currentTimeMillis()
)
