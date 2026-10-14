package com.example.intrusiondetector.helpers

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Клас-помічник для роботи з Android Sensor API.
 * Перетворює зчитування акселерометра у реактивний потік Flow<FloatArray>.
 */
class SensorReader(context: Context) {
    private val manager = context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor: Sensor? = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    val sensorName: String = sensor?.name ?: "Акселерометр відсутній"

    fun readings(): Flow<FloatArray> = callbackFlow {
        if (sensor == null) {
            close()
            return@callbackFlow
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                if (e.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    trySend(e.values.clone())
                }
            }

            override fun onAccuracyChanged(s: Sensor?, a: Int) = Unit
        }

        manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)

        // викликається при скасуванні Flow → скасовує реєстрацію слухача (unregisterListener)
        awaitClose {
            manager.unregisterListener(listener)
        }
    }
}
