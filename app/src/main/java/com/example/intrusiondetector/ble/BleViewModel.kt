package com.example.intrusiondetector.ble

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.intrusiondetector.helpers.IBleConnector
import com.example.intrusiondetector.helpers.MockBleConnector
import com.example.intrusiondetector.helpers.MockBleScanner
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel для управління екраном Bluetooth (ЛР 2).
 * Отримує Action, змінює State, надсилає Event.
 */
class BleViewModel(app: Application) : AndroidViewModel(app) {

    private val bleScanner = MockBleScanner()
    private val bleConnector: IBleConnector = MockBleConnector()

    private val _state = MutableStateFlow(BleState())
    val state: StateFlow<BleState> = _state.asStateFlow()

    private val _events = Channel<BleEvent>()
    val events = _events.receiveAsFlow()

    private var scanJob: Job? = null
    private var connectJob: Job? = null

    init {
        // Відстежуємо логи та стан підключення від IBleConnector
        viewModelScope.launch {
            bleConnector.logs.collect { newLogs ->
                _state.update { it.copy(logs = newLogs) }
            }
        }
        viewModelScope.launch {
            bleConnector.connectionState.collect { newState ->
                _state.update { it.copy(connectionState = newState) }
            }
        }
    }

    /**
     * Вхід у ViewModel — всі дії з UI приходять сюди
     */
    fun onAction(action: BleAction) {
        when (action) {
            BleAction.StartScan -> startScan()
            is BleAction.ConnectDevice -> connectDevice(action.device)
            BleAction.Disconnect -> disconnectDevice()
            is BleAction.UpdateCommandText -> {
                _state.update { it.copy(commandInputText = action.text) }
            }
            is BleAction.SendCommand -> sendCommand(action.command)
            BleAction.ClearLogs -> bleConnector.clearLogs()
        }
    }

    private fun startScan() {
        if (_state.value.isScanning) return
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            bleScanner.scanDevices().collect { (isScanning, devices) ->
                _state.update {
                    it.copy(
                        isScanning = isScanning,
                        discoveredDevices = devices
                    )
                }
                if (!isScanning && devices.isNotEmpty()) {
                    _events.send(BleEvent.ShowMessage("Знайдено пристроїв: ${devices.size}"))
                }
            }
        }
    }

    private fun connectDevice(device: DiscoveredDevice) {
        connectJob?.cancel()
        _state.update { it.copy(selectedDevice = device) }
        connectJob = viewModelScope.launch {
            val success = bleConnector.connect(device.address)
            if (success) {
                _events.send(BleEvent.ShowMessage("Підключено до ${device.name}"))
            } else {
                _events.send(BleEvent.ShowMessage("Помилка підключення"))
            }
        }
    }

    private fun disconnectDevice() {
        connectJob?.cancel()
        bleConnector.disconnect()
        _state.update { it.copy(selectedDevice = null) }
        viewModelScope.launch {
            _events.send(BleEvent.ShowMessage("Відключено від BLE пристрою"))
        }
    }

    private fun sendCommand(command: String) {
        if (command.isBlank()) return
        viewModelScope.launch {
            val success = bleConnector.sendData(command)
            if (success) {
                _events.send(BleEvent.ShowMessage("Відправлено команду: $command"))
            } else {
                _events.send(BleEvent.ShowMessage("Не вдалося відправити команду. Перевірте з'єднання"))
            }
        }
    }
}
