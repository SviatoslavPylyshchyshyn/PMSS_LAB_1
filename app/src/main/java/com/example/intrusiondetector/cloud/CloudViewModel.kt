package com.example.intrusiondetector.cloud

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.intrusiondetector.helpers.SensorReader
import com.example.intrusiondetector.helpers.SmartActuator
import com.example.intrusiondetector.monitor.MonitorState
import com.example.intrusiondetector.repository.SensorPayload
import com.example.intrusiondetector.repository.SensorRepository
import com.example.intrusiondetector.repository.SyncStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * ViewModel для управління хмарною синхронізацією з Firebase (ЛР 3).
 */
class CloudViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = SensorRepository(app)
    private val sensorReader = SensorReader(app)
    private val smartActuator = SmartActuator.getInstance(app)

    private val _state = MutableStateFlow(CloudState())
    val state: StateFlow<CloudState> = _state.asStateFlow()

    private val _events = Channel<CloudEvent>()
    val events = _events.receiveAsFlow()

    private var autoSyncJob: Job? = null
    private var sensorJob: Job? = null

    private var latestX = 0f
    private var latestY = 0f
    private var latestZ = 0f

    init {
        // Відстежуємо стан синхронізації та історію з репозиторію
        viewModelScope.launch {
            repository.syncStatus.collect { status ->
                _state.update { it.copy(syncStatus = status) }
            }
        }
        viewModelScope.launch {
            repository.history.collect { historyList ->
                _state.update { it.copy(history = historyList) }
            }
        }

        // запуск відстеження актуальних показників акселерометра
        startSensorTracking()

        // запуск автоматичної періодичної відправки даних кожні 5 секунд
        startAutoSync()
    }

    fun onAction(action: CloudAction) {
        when (action) {
            CloudAction.ToggleAutoSync -> {
                val newAutoSync = !_state.value.isAutoSyncActive
                _state.update { it.copy(isAutoSyncActive = newAutoSync) }
                if (newAutoSync) {
                    startAutoSync()
                } else {
                    autoSyncJob?.cancel()
                }
            }

            CloudAction.ToggleSimulatedOffline -> {
                val newOffline = !_state.value.isSimulatedOffline
                _state.update { it.copy(isSimulatedOffline = newOffline) }
                viewModelScope.launch {
                    _events.send(
                        CloudEvent.ShowMessage(
                            if (newOffline) "Імітація відсутності мережі (Offline Mode)"
                            else "Мережеве з'єднання відновлено"
                        )
                    )
                }
            }

            CloudAction.TriggerManualSync -> {
                performSync()
            }
        }
    }

    private fun startSensorTracking() {
        sensorJob?.cancel()
        sensorJob = viewModelScope.launch(Dispatchers.IO) {
            sensorReader.readings().collect { v ->
                latestX = v[0]
                latestY = v[1]
                latestZ = v[2]
            }
        }
    }

    private fun startAutoSync() {
        autoSyncJob?.cancel()
        autoSyncJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(_state.value.autoSyncIntervalSeconds * 1000L)
                if (_state.value.isAutoSyncActive) {
                    performSync()
                }
            }
        }
    }

    private fun performSync() {
        viewModelScope.launch(Dispatchers.IO) {
            val isSimulatedOffline = _state.value.isSimulatedOffline
            val hasRealNetwork = repository.isNetworkAvailable()

            val monitorState = MonitorState(x = latestX, y = latestY, z = latestZ)

            // Передаємо ТРИВОГУ поки активована сирена або спалах у системі
            val computedIntrusionState = when {
                smartActuator.isSirenPlaying || smartActuator.isFlashlightOn -> "ALARM"
                smartActuator.isVibrating -> "WARNING"
                else -> monitorState.intrusionState.name
            }

            val currentPayload = SensorPayload(
                sensorType = "ACCELEROMETER",
                deviceId = "Smartphone-Sensor-01",
                x = latestX,
                y = latestY,
                z = latestZ,
                magnitude = monitorState.magnitude,
                intrusionState = computedIntrusionState,
                timestamp = System.currentTimeMillis()
            )

            if (isSimulatedOffline || !hasRealNetwork) {
                _state.update { it.copy(syncStatus = SyncStatus.OFFLINE_BUFFERED) }
                _events.send(CloudEvent.ShowMessage("Мережа відсутня. Дані збережено в буфер (Pending)."))
                repository.saveToCloud(currentPayload)
            } else {
                val result = repository.saveToCloud(currentPayload)
                result.onSuccess {
                    _state.update { it.copy(lastSyncTimestamp = System.currentTimeMillis()) }
                }.onFailure {
                    _events.send(CloudEvent.ShowMessage("Помилка відправки у Firebase"))
                }
            }
        }
    }
}
