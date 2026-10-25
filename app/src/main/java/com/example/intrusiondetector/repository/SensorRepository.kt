package com.example.intrusiondetector.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/**
 * Репозиторій для роботи з хмарним сховищем (Firebase Realtime Database).
 * Зберігає дані асинхронно у Dispatchers.IO, перевіряє стан мережі та веде локальний буфер.
 */
class SensorRepository(private val context: Context) {

    private val tag = "SensorRepository"
    private val databaseUrl = "https://intrusiondetector-default-rtdb.europe-west1.firebasedatabase.app/"

    private val _syncStatus = MutableStateFlow(SyncStatus.SYNCED)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _history = MutableStateFlow<List<SensorPayload>>(emptyList())
    val history: StateFlow<List<SensorPayload>> = _history.asStateFlow()

    private val offlineQueue = mutableListOf<SensorPayload>()

    // Firebase Database Reference
    private val firebaseDatabase by lazy {
        try {
            FirebaseDatabase.getInstance(databaseUrl).getReference("sensor_payloads")
        } catch (e: Exception) {
            Log.w(tag, "Firebase init fallback: ${e.message}")
            null
        }
    }

    /**
     * Відправка пакета сенсорних даних у хмару Firebase у фоновому потоці Dispatchers.IO.
     */
    suspend fun saveToCloud(payload: SensorPayload): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (!isNetworkAvailable()) {
                _syncStatus.value = SyncStatus.OFFLINE_BUFFERED
                offlineQueue.add(payload)
                addHistory(payload)
                Log.w(tag, "Network unavailable. Payload buffered offline. Queue size: ${offlineQueue.size}")
                throw IllegalStateException("Відсутній інтернет. Запис збережено локально (Pending).")
            }

            _syncStatus.value = SyncStatus.PENDING

            // Відправка у Firebase (SDK або REST)
            val databaseRef = firebaseDatabase
            if (databaseRef != null) {
                val key = databaseRef.push().key ?: payload.timestamp.toString()
                databaseRef.child(key).setValue(payload)
            } else {
                sendViaRestPost(payload)
            }

            // Вивантажуємо накопичений офлайн буфер при відновленні мережі
            if (offlineQueue.isNotEmpty()) {
                val pendingList = ArrayList(offlineQueue)
                offlineQueue.clear()
                pendingList.forEach { pendingPayload ->
                    if (databaseRef != null) {
                        val key = databaseRef.push().key ?: pendingPayload.timestamp.toString()
                        databaseRef.child(key).setValue(pendingPayload)
                    } else {
                        sendViaRestPost(pendingPayload)
                    }
                }
                Log.d(tag, "Synced ${pendingList.size} buffered offline payloads to Firebase.")
            }

            _syncStatus.value = SyncStatus.SYNCED
            addHistory(payload)
            Log.d(tag, "Successfully uploaded payload to Firebase: $payload")
            Unit
        }.onFailure { error ->
            if (_syncStatus.value != SyncStatus.OFFLINE_BUFFERED) {
                _syncStatus.value = SyncStatus.ERROR
            }
            Log.e(tag, "Cloud sync failed: ${error.message}")
        }
    }

    private fun sendViaRestPost(payload: SensorPayload) {
        try {
            val url = URL("${databaseUrl}sensor_payloads.json")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json; utf-8")
            conn.doOutput = true
            conn.connectTimeout = 5000
            conn.readTimeout = 5000

            val jsonBody = String.format(
                Locale.US,
                "{\"sensorType\":\"%s\",\"deviceId\":\"%s\",\"x\":%.2f,\"y\":%.2f,\"z\":%.2f,\"magnitude\":%.2f,\"intrusionState\":\"%s\",\"timestamp\":%d}",
                payload.sensorType,
                payload.deviceId,
                payload.x,
                payload.y,
                payload.z,
                payload.magnitude,
                payload.intrusionState,
                payload.timestamp
            )

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(jsonBody)
                writer.flush()
            }

            val responseCode = conn.responseCode
            Log.d(tag, "Firebase REST POST Response Code: $responseCode")
            conn.disconnect()
        } catch (e: Exception) {
            Log.w(tag, "Firebase REST POST Warning: ${e.message}")
        }
    }

    private fun addHistory(payload: SensorPayload) {
        _history.update { current ->
            (listOf(payload) + current).take(20)
        }
    }

    /**
     * Перевірка наявності активного підключення до Інтернету.
     */
    fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
