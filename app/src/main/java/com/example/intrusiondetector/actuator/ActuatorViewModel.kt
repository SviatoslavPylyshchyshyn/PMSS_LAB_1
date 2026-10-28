package com.example.intrusiondetector.actuator

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.intrusiondetector.helpers.DecisionEngine
import com.example.intrusiondetector.helpers.SensorReader
import com.example.intrusiondetector.helpers.SmartActuator
import com.example.intrusiondetector.monitor.MonitorState
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ViewModel для управління виконавчими пристроями (актуаторами) та обробкою зворотного зв'язку (ЛР 4).
 */
class ActuatorViewModel(app: Application) : AndroidViewModel(app) {

    private val tag = "ActuatorViewModel"
    private val databaseUrl = "https://intrusiondetector-default-rtdb.europe-west1.firebasedatabase.app/"

    val smartActuator = SmartActuator.getInstance(app)
    val decisionEngine = DecisionEngine(smartActuator)
    private val sensorReader = SensorReader(app)

    private val _state = MutableStateFlow(
        ActuatorState(hasFlashFeature = smartActuator.hasFlashFeature)
    )
    val state: StateFlow<ActuatorState> = _state.asStateFlow()

    private val _events = Channel<ActuatorEvent>()
    val events = _events.receiveAsFlow()

    private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private var sensorJob: Job? = null

    init {
        // дистанційне керування (Cloud-to-Device): слухання гілки Firebase /control/actuators
        listenToFirebaseCloudControl()

        // локальне автоматичне керування через DecisionEngine
        startDecisionEngineSensorLoop()
    }

    fun onAction(action: ActuatorAction) {
        when (action) {
            ActuatorAction.ToggleFlashlight -> {
                val newStatus = !_state.value.isFlashlightOn
                val success = smartActuator.toggleFlashlight(newStatus, viewModelScope)
                updateActuatorState()
                addLog(
                    if (success) "Спалах ${if (newStatus) "УВІМКНЕНО (Блимання)" else "ВИМКНЕНО"}"
                    else "Спалах недоступний у даній системі/емуляторі"
                )
            }

            ActuatorAction.ToggleSiren -> {
                val newStatus = !_state.value.isSirenPlaying
                smartActuator.toggleSiren(newStatus)
                updateActuatorState()
                addLog("Аудіо-сирену ${if (newStatus) "УВІМКНЕНО" else "ВИМКНЕНО"}")
            }

            ActuatorAction.ToggleVibration -> {
                val newStatus = !_state.value.isVibrating
                smartActuator.toggleVibration(newStatus)
                updateActuatorState()
                addLog("Вібрацію ${if (newStatus) "УВІМКНЕНО" else "ВИМКНЕНО"}")
            }

            ActuatorAction.TriggerFullAlarm -> {
                smartActuator.triggerAlarm(viewModelScope)
                updateActuatorState()
                addLog("ПРИМУСОВО АКТИВОВАНО ПОВНУ ТРИВОГУ")
            }

            ActuatorAction.StopAllActuators -> {
                smartActuator.stopAlarm()
                updateActuatorState()
                addLog("УСІ АКТУАТОРИ ЗУПИНЕНО")
            }

            ActuatorAction.ToggleDecisionEngine -> {
                val newEnabled = !_state.value.isDecisionEngineEnabled
                decisionEngine.isEnabled = newEnabled
                _state.update { it.copy(isDecisionEngineEnabled = newEnabled) }
                addLog("Автоматичний DecisionEngine ${if (newEnabled) "УВІМКНЕНО" else "ВИМКНЕНО"}")
            }

            ActuatorAction.ClearLogs -> {
                _state.update { it.copy(logs = emptyList()) }
            }
        }
    }

    private fun startDecisionEngineSensorLoop() {
        sensorJob?.cancel()
        sensorJob = viewModelScope.launch(Dispatchers.IO) {
            sensorReader.readings().collect { v ->
                val monitorState = MonitorState(x = v[0], y = v[1], z = v[2])

                val wasSirenOn = smartActuator.isSirenPlaying
                val wasFlashlightOn = smartActuator.isFlashlightOn
                val wasVibrating = smartActuator.isVibrating

                decisionEngine.processSensorState(monitorState)

                // Логування автоматичних дій DecisionEngine
                if (!wasSirenOn && smartActuator.isSirenPlaying) {
                    addLog("DecisionEngine (Рух/Удар): АКТИВОВАНО Сирену [ALARM]")
                }
                if (!wasFlashlightOn && smartActuator.isFlashlightOn) {
                    addLog("DecisionEngine (Рух/Удар): АКТИВОВАНО Спалах [ALARM]")
                }
                if (!wasVibrating && smartActuator.isVibrating) {
                    addLog("DecisionEngine (Вібрація): АКТИВОВАНО Вібро [WARNING]")
                }
                if (wasVibrating && !smartActuator.isVibrating && !smartActuator.isSirenPlaying) {
                    addLog("DecisionEngine (15с спокою): ВИМКНЕНО Вібро [SAFE]")
                }

                updateActuatorState()
            }
        }
    }

    private fun listenToFirebaseCloudControl() {
        try {
            val controlRef = FirebaseDatabase.getInstance(databaseUrl).getReference("control/actuators")
            controlRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val alarmStatus = snapshot.child("alarm_status").getValue(Boolean::class.java)
                    val sirenStatus = snapshot.child("siren_status").getValue(Boolean::class.java) ?: false
                    val flashlightStatus = snapshot.child("flashlight_status").getValue(Boolean::class.java) ?: false

                    Log.d(tag, "Firebase Cloud-to-Device update: alarm=$alarmStatus, siren=$sirenStatus, flashlight=$flashlightStatus")

                    if (alarmStatus != null) {
                        if (alarmStatus) {
                            if (!smartActuator.isSirenPlaying || !smartActuator.isFlashlightOn) {
                                smartActuator.triggerAlarm(viewModelScope)
                                addLog("Cloud-to-Device: АКТИВОВАНО ПОВНУ ТРИВОГУ [alarm_status = true]")
                            }
                        } else {
                            if (smartActuator.isSirenPlaying || smartActuator.isFlashlightOn || smartActuator.isVibrating) {
                                smartActuator.stopAlarm()
                                addLog("Cloud-to-Device: ТРИВОГУ ЗУПИНЕНО [alarm_status = false]")
                            }
                        }
                    } else {
                        if (sirenStatus != smartActuator.isSirenPlaying) {
                            smartActuator.toggleSiren(sirenStatus)
                            addLog("Cloud-to-Device: Сирена -> $sirenStatus")
                        }
                        if (flashlightStatus != smartActuator.isFlashlightOn) {
                            smartActuator.toggleFlashlight(flashlightStatus, viewModelScope)
                            addLog("Cloud-to-Device: Спалах -> $flashlightStatus")
                        }
                    }

                    updateActuatorState()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(tag, "Firebase control listener error: ${error.message}")
                }
            })
        } catch (e: Exception) {
            Log.w(tag, "Firebase control init warning: ${e.message}")
        }
    }

    private fun updateActuatorState() {
        val isAnyActive = smartActuator.isSirenPlaying || smartActuator.isFlashlightOn || smartActuator.isVibrating
        val lampStatus = when {
            smartActuator.isSirenPlaying && smartActuator.isFlashlightOn -> "ALARM TRIGGERED (RED)"
            isAnyActive -> "ACTIVE / WARNING (AMBER)"
            else -> "NORMAL (GREEN)"
        }
        val lampColorHex = when {
            smartActuator.isSirenPlaying && smartActuator.isFlashlightOn -> 0xFFE53935 // Red
            isAnyActive -> 0xFFFFB300 // оранжевий
            else -> 0xFF4CAF50 // зелений
        }

        _state.update {
            it.copy(
                isFlashlightOn = smartActuator.isFlashlightOn,
                isSirenPlaying = smartActuator.isSirenPlaying,
                isVibrating = smartActuator.isVibrating,
                virtualLampStatus = lampStatus,
                virtualLampColorHex = lampColorHex
            )
        }
    }

    private fun addLog(message: String) {
        val timestamp = dateFormat.format(Date())
        val logLine = "[$timestamp] $message"
        _state.update { current ->
            current.copy(logs = (current.logs + logLine).takeLast(20))
        }
    }
}
