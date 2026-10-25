package com.example.intrusiondetector.cloud

import com.example.intrusiondetector.repository.SensorPayload
import com.example.intrusiondetector.repository.SyncStatus

/**
 * State — стан екрана хмарної синхронізації (ЛР 3).
 */
data class CloudState(
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val history: List<SensorPayload> = emptyList(),
    val isAutoSyncActive: Boolean = true,
    val autoSyncIntervalSeconds: Int = 5,
    val isSimulatedOffline: Boolean = false,
    val lastSyncTimestamp: Long = System.currentTimeMillis()
)

/**
 * Action — дії користувача на екрані хмари.
 */
sealed interface CloudAction {
    data object ToggleAutoSync : CloudAction
    data object ToggleSimulatedOffline : CloudAction
    data object TriggerManualSync : CloudAction
}

/**
 * Event — разові сповіщення від ViewModel (Toast).
 */
sealed interface CloudEvent {
    data class ShowMessage(val text: String) : CloudEvent
}
