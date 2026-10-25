package com.example.intrusiondetector.repository

/**
 * Стан хмарної синхронізації (Cloud Sync Status).
 */
enum class SyncStatus {
    SYNCED,            // зелений: дані успішно синхронізовано з хмарою
    PENDING,           // жовтий: відправка даних у фоні
    OFFLINE_BUFFERED,  // червоний: немає інтернету, збережено в локальний буфер
    ERROR              // червоний: помилка мережі / сервер недоступний
}
