package com.example.intrusiondetector.monitor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.intrusiondetector.helpers.SensorReader
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel: отримує Action, змінює State, надсилає Event.
 */
class MonitorViewModel(app: Application) : AndroidViewModel(app) {

    // клас-помічник для роботи з Android Sensor API
    private val sensorReader = SensorReader(app)

    // State: приватний змінюваний + публічний тільки для читання
    private val _state = MutableStateFlow(
        MonitorState(sensorName = sensorReader.sensorName)
    )
    val state: StateFlow<MonitorState> = _state.asStateFlow()

    // Event: Channel доставляє кожну подію рівно один раз
    private val _events = Channel<MonitorEvent>()
    val events = _events.receiveAsFlow()

    private var sensorJob: Job? = null

    private var filteredX = 0f
    private var filteredY = 0f
    private var filteredZ = 0f

    /**
     * Вхід у ViewModel — всі дії приходять сюди
     */
    fun onAction(action: MonitorAction) {
        when (action) {
            MonitorAction.Start -> startSensor()
            MonitorAction.Stop -> stopSensor()
            MonitorAction.ToggleFilter -> {
                val newFilter = !_state.value.isFilterEnabled
                _state.update { it.copy(isFilterEnabled = newFilter) }
                viewModelScope.launch {
                    _events.send(
                        MonitorEvent.ShowMessage(
                            if (newFilter) "Фільтрація увімкнена" else "Сирі дані"
                        )
                    )
                }
            }
        }
    }

    private fun startSensor() {
        if (sensorJob != null) return
        _state.update { it.copy(isRunning = true, sensorName = sensorReader.sensorName) }
        sensorJob = viewModelScope.launch {
            sensorReader.readings().collect { v ->
                val rawX = v[0]
                val rawY = v[1]
                val rawZ = v[2]

                if (_state.value.isFilterEnabled) {
                    val alpha = 0.2f
                    filteredX += alpha * (rawX - filteredX)
                    filteredY += alpha * (rawY - filteredY)
                    filteredZ += alpha * (rawZ - filteredZ)
                } else {
                    filteredX = rawX
                    filteredY = rawY
                    filteredZ = rawZ
                }

                _state.update {
                    it.copy(
                        x = filteredX,
                        y = filteredY,
                        z = filteredZ
                    )
                }
            }
        }
    }

    private fun stopSensor() {
        sensorJob?.cancel() // скасування Flow → unregisterListener()
        sensorJob = null
        _state.update { it.copy(isRunning = false) }
    }
}
