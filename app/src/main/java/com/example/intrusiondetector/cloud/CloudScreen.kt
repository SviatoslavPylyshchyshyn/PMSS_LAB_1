package com.example.intrusiondetector.cloud

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.intrusiondetector.repository.SensorPayload
import com.example.intrusiondetector.repository.SyncStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Екран хмарної синхронізації Firebase (ЛР 3).
 */
@Composable
fun CloudScreen(vm: CloudViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            when (event) {
                is CloudEvent.ShowMessage ->
                    Toast.makeText(context, event.text, Toast.LENGTH_SHORT).show()
            }
        }
    }

    CloudContent(state = state, onAction = vm::onAction)
}

/**
 * Чиста Composable-функція: State на вході, Action на виході.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudContent(
    state: CloudState,
    onAction: (CloudAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Хмарна синхронізація",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // індикатор стану "Хмарна синхронізація" (Зелений при успіху / Червоний при помилці чи офлайні)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RectangleShape,
                colors = CardDefaults.cardColors(
                    containerColor = when (state.syncStatus) {
                        SyncStatus.SYNCED -> Color(0xFFE8F5E9)          // Зелений фон
                        SyncStatus.PENDING -> Color(0xFFFFF8E1)         // Жовтий фон
                        SyncStatus.OFFLINE_BUFFERED -> Color(0xFFFFEBEE)// Червоний фон
                        SyncStatus.ERROR -> Color(0xFFFFEBEE)            // Червоний фон
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(
                                when (state.syncStatus) {
                                    SyncStatus.SYNCED -> Color(0xFF4CAF50)          // зелений
                                    SyncStatus.PENDING -> Color(0xFFFFB300)         // жовтий
                                    SyncStatus.OFFLINE_BUFFERED -> Color(0xFFE53935) // червоний
                                    SyncStatus.ERROR -> Color(0xFFE53935)            // червоний
                                }
                            )
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Хмарна синхронізація: ${
                                when (state.syncStatus) {
                                    SyncStatus.SYNCED -> "СИНХРОНІЗОВАНО (Firebase 200 OK)"
                                    SyncStatus.PENDING -> "ПЕРЕДАЧА ДАНИХ (У процесі...)"
                                    SyncStatus.OFFLINE_BUFFERED -> "НЕМАЄ МЕРЕЖІ / PENDING (Збережено локально)"
                                    SyncStatus.ERROR -> "ПОМИЛКА МЕРЕЖІ (Сервер недоступний)"
                                }
                            }",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "Останнє оновлення: ${dateFormat.format(Date(state.lastSyncTimestamp))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // налаштування авто-синхронізації та імітації офлайн режиму
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RectangleShape,
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Налаштування передачі даних",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // автоматична синхронізація кожні 5 секунд
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = "Авто-синхронізація (кожні 5 сек)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (state.isAutoSyncActive) "Періодична відправка у хмару" else "Зупинено",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Box(
                            modifier = Modifier.width(52.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Switch(
                                checked = state.isAutoSyncActive,
                                onCheckedChange = { onAction(CloudAction.ToggleAutoSync) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // імітація відсутності мережі (Offline mode)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = "Імітація відсутності мережі",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (state.isSimulatedOffline) "Буферизація локально (Pending)" else "Звичайне підключення",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Box(
                            modifier = Modifier.width(52.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Switch(
                                checked = state.isSimulatedOffline,
                                onCheckedChange = { onAction(CloudAction.ToggleSimulatedOffline) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { onAction(CloudAction.TriggerManualSync) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RectangleShape
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Синхронізувати зараз (Примусово)")
                    }
                }
            }

            // історія вивантажених JSON записів (Firebase payload history)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RectangleShape,
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E1E1E)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Історія вивантаження JSON (Firebase Realtime DB):",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.Green
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(Color.Black)
                            .border(1.dp, Color.DarkGray, RectangleShape),
                        color = Color.Black
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            if (state.history.isEmpty()) {
                                Text(
                                    text = "> Очікування першого пакета даних...",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.Gray
                                )
                            } else {
                                state.history.forEach { payload ->
                                    PayloadItemRow(payload = payload, dateFormat = dateFormat)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PayloadItemRow(payload: SensorPayload, dateFormat: SimpleDateFormat) {
    val timestampStr = dateFormat.format(Date(payload.timestamp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = "[$timestampStr] POST -> /sensor_payloads.json",
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = Color(0xFF81D4FA)
        )
        Text(
            text = "  JSON: {\"type\":\"${payload.sensorType}\", \"X\":${String.format(Locale.US, "%.2f", payload.x)}, \"Y\":${String.format(Locale.US, "%.2f", payload.y)}, \"Z\":${String.format(Locale.US, "%.2f", payload.z)}}",
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = Color(0xFFA5D6A7)
        )
    }
}
