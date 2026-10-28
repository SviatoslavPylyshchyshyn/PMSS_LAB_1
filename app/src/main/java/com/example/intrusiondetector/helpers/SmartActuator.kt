package com.example.intrusiondetector.helpers

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.example.intrusiondetector.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Клас-помічник SmartActuator для керування виконавчими пристроями (актуаторами):
 * - Справжній ліхтарик (CameraManager.setTorchMode) з підтримкою блимання (стробоскоп)
 * - Справжня сирена (MediaPlayer із звуковим файлом siren.mp3)
 * - Справжня вібрація (VibratorManager / Vibrator)
 */
class SmartActuator(context: Context) {

    private val context: Context = context.applicationContext

    companion object {
        @Volatile
        private var INSTANCE: SmartActuator? = null

        fun getInstance(context: Context): SmartActuator {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SmartActuator(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val tag = "SmartActuator"

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private var cameraId: String? = null
    val hasFlashFeature: Boolean = context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)

    private var mediaPlayer: MediaPlayer? = null
    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    private var flashlightBlinkJob: Job? = null

    var isFlashlightOn: Boolean = false
        private set

    var isSirenPlaying: Boolean = false
        private set

    var isVibrating: Boolean = false
        private set

    init {
        findCameraWithFlash()
    }

    private fun findCameraWithFlash() {
        if (cameraManager == null) return
        try {
            val cameraIds = cameraManager.cameraIdList
            if (cameraIds.isNotEmpty()) {
                for (id in cameraIds) {
                    val characteristics = cameraManager.getCameraCharacteristics(id)
                    val flashAvailable = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
                    if (flashAvailable) {
                        cameraId = id
                        break
                    }
                }
                if (cameraId == null) {
                    cameraId = cameraIds[0]
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "CameraManager error: ${e.message}")
        }
    }

    /**
     * Керування ліхтариком (CameraManager.setTorchMode).
     * Якщо передано scope, запускається ритмічне блимання (стробоскоп кожні 300мс).
     */
    fun toggleFlashlight(status: Boolean, scope: CoroutineScope? = null): Boolean {
        flashlightBlinkJob?.cancel()
        flashlightBlinkJob = null

        isFlashlightOn = status

        if (status && scope != null) {
            // Запуск корутини миготіння/блимання спалаху (стробоскоп)
            flashlightBlinkJob = scope.launch(Dispatchers.IO) {
                var toggleState = true
                while (isActive && isFlashlightOn) {
                    setHardwareTorch(toggleState)
                    toggleState = !toggleState
                    delay(300) // Блимає кожні 300мс
                }
                setHardwareTorch(false)
            }
        } else {
            setHardwareTorch(status)
        }
        return true
    }

    private fun setHardwareTorch(enabled: Boolean) {
        if (cameraManager != null && cameraId != null) {
            try {
                cameraManager.setTorchMode(cameraId!!, enabled)
                Log.d(tag, "Hardware Camera Flashlight setTorchMode: $enabled")
            } catch (e: Exception) {
                Log.d(tag, "Virtual Flashlight toggled (Emulator Camera mode): $enabled")
            }
        } else {
            Log.d(tag, "Virtual Flashlight toggled: $enabled")
        }
    }

    /**
     * Керування відтворенням аудіо-сирени (siren.mp3 у res/raw).
     */
    fun toggleSiren(status: Boolean) {
        try {
            if (status) {
                if (mediaPlayer == null) {
                    mediaPlayer = MediaPlayer.create(context, R.raw.siren)?.apply {
                        isLooping = true
                    }
                }
                if (mediaPlayer?.isPlaying == false) {
                    mediaPlayer?.start()
                }
                isSirenPlaying = true
                Log.d(tag, "Siren audio started.")
            } else {
                mediaPlayer?.stop()
                mediaPlayer?.release()
                mediaPlayer = null
                isSirenPlaying = false
                Log.d(tag, "Siren audio stopped.")
            }
        } catch (e: Exception) {
            Log.e(tag, "Siren player error: ${e.message}")
            isSirenPlaying = false
        }
    }

    /**
     * Керування вібровідгуком (VibratorManager / Vibrator).
     */
    fun toggleVibration(status: Boolean) {
        try {
            if (status) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val pattern = longArrayOf(0, 500, 200, 500)
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(longArrayOf(0, 500, 200, 500), 0)
                }
                isVibrating = true
                Log.d(tag, "Vibration started.")
            } else {
                vibrator.cancel()
                isVibrating = false
                Log.d(tag, "Vibration stopped.")
            }
        } catch (e: Exception) {
            Log.e(tag, "Vibrator error: ${e.message}")
            isVibrating = false
        }
    }

    /**
     * Активація повної тривоги (Сирена + Вібрація + Блимаючий Спалах).
     */
    fun triggerAlarm(scope: CoroutineScope? = null) {
        toggleSiren(true)
        toggleVibration(true)
        toggleFlashlight(true, scope)
        Log.d(tag, "FULL ALARM TRIGGERED!")
    }

    /**
     * Зупинка всіх тривожних актуаторів.
     */
    fun stopAlarm() {
        toggleSiren(false)
        toggleVibration(false)
        toggleFlashlight(false)
        Log.d(tag, "ALL ALARMS STOPPED.")
    }
}
