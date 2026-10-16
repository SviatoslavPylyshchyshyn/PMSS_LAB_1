package com.example.intrusiondetector.ble

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.material3.Surface
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

/**
 * Екран управління Bluetooth Interface Layer (ЛР 2).
 */
@Composable
fun BleScreen(vm: BleViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            when (event) {
                is BleEvent.ShowMessage ->
                    Toast.makeText(context, event.text, Toast.LENGTH_SHORT).show()
            }
        }
    }

    BleContent(state = state, onAction = vm::onAction)
}

/**
 * Чиста Composable-функція: State на вході, Action на виході.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BleContent(
    state: BleState,
    onAction: (BleAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "BLE Смарт-Хаб",
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
            // Кнопка сканування пристроїв
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RectangleShape,
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Пошук BLE Пристроїв",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Button(
                            onClick = { onAction(BleAction.StartScan) },
                            enabled = !state.isScanning,
                            shape = RectangleShape
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (state.isScanning) "Сканування..." else "Пошук")
                        }
                    }

                    if (state.isScanning) {
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            strokeCap = StrokeCap.Square
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Пошук віртуальних BLE пристроїв (затримка 2с)...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // список знайдених пристроїв
            if (state.discoveredDevices.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RectangleShape,
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Знайдені пристрої (${state.discoveredDevices.size}):",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        state.discoveredDevices.forEach { device ->
                            DeviceItemCard(
                                device = device,
                                isConnected = state.selectedDevice?.address == device.address &&
                                        state.connectionState == BleConnectionState.CONNECTED,
                                isConnecting = state.selectedDevice?.address == device.address &&
                                        state.connectionState == BleConnectionState.CONNECTING,
                                onConnectClick = { onAction(BleAction.ConnectDevice(device)) },
                                onDisconnectClick = { onAction(BleAction.Disconnect) }
                            )
                        }
                    }
                }
            }

            // індикатор статусу з'єднання
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RectangleShape,
                colors = CardDefaults.cardColors(
                    containerColor = when (state.connectionState) {
                        BleConnectionState.CONNECTED -> Color(0xFFE8F5E9)
                        BleConnectionState.CONNECTING -> Color(0xFFFFF8E1)
                        BleConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.surfaceVariant.copy(
                            alpha = 0.5f
                        )
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
                            .size(14.dp)
                            .background(
                                when (state.connectionState) {
                                    BleConnectionState.CONNECTED -> Color(0xFF4CAF50)
                                    BleConnectionState.CONNECTING -> Color(0xFFFFB300)
                                    BleConnectionState.DISCONNECTED -> Color(0xFFE53935)
                                }
                            )
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Статус з'єднання: ${
                                when (state.connectionState) {
                                    BleConnectionState.CONNECTED -> "CONNECTED (Підключено)"
                                    BleConnectionState.CONNECTING -> "CONNECTING (Підключення...)"
                                    BleConnectionState.DISCONNECTED -> "DISCONNECTED (Відключено)"
                                }
                            }",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (state.selectedDevice != null) {
                            Text(
                                text = "Пристрій: ${state.selectedDevice.name} [${state.selectedDevice.address}]",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // відправка команд на пристрій
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RectangleShape,
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Панель керування та відправки команд",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // швидкі кнопки команд
                    Text(
                        text = "Швидкі команди:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onAction(BleAction.SendCommand("ARM_SYSTEM")) },
                            enabled = state.connectionState == BleConnectionState.CONNECTED,
                            shape = RectangleShape,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("ARM", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = { onAction(BleAction.SendCommand("DISARM")) },
                            enabled = state.connectionState == BleConnectionState.CONNECTED,
                            shape = RectangleShape,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("DISARM", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = { onAction(BleAction.SendCommand("SEND_SENSOR_DATA")) },
                            enabled = state.connectionState == BleConnectionState.CONNECTED,
                            shape = RectangleShape,
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Text("SENSOR", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // поле довільного вводу команди
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = state.commandInputText,
                            onValueChange = { onAction(BleAction.UpdateCommandText(it)) },
                            label = { Text("Команда BLE") },
                            modifier = Modifier.weight(1f),
                            shape = RectangleShape,
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = { onAction(BleAction.SendCommand(state.commandInputText)) },
                            enabled = state.connectionState == BleConnectionState.CONNECTED &&
                                    state.commandInputText.isNotBlank(),
                            shape = RectangleShape
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Відправити")
                        }
                    }
                }
            }

            // термінал логування обміну повідомленнями
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Термінал логування (Logcat & UI):",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.Green
                        )

                        IconButton(
                            onClick = { onAction(BleAction.ClearLogs) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Очистити",
                                tint = Color.LightGray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
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
                            if (state.logs.isEmpty()) {
                                Text(
                                    text = "> Логи обміну відсутні. Підключіться до пристрою...",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.Gray
                                )
                            } else {
                                state.logs.forEach { logLine ->
                                    Text(
                                        text = logLine,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = when {
                                            logLine.contains("TX") -> Color(0xFF81D4FA) // Light blue
                                            logLine.contains("RX") -> Color(0xFFA5D6A7) // Light green
                                            logLine.contains("✔") -> Color.Green
                                            logLine.contains("✖") || logLine.contains("⚠") -> Color(
                                                0xFFEF9A9A
                                            ) // Light red
                                            else -> Color.White
                                        }
                                    )
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
private fun DeviceItemCard(
    device: DiscoveredDevice,
    isConnected: Boolean,
    isConnecting: Boolean,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RectangleShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "MAC: ${device.address}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "RSSI: ${device.rssi} dBm",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isConnected) {
                OutlinedButton(
                    onClick = onDisconnectClick,
                    shape = RectangleShape,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                ) {
                    Text("Відключити")
                }
            } else {
                Button(
                    onClick = onConnectClick,
                    enabled = !isConnecting,
                    shape = RectangleShape
                ) {
                    Text(if (isConnecting) "З'єднання..." else "Підключити")
                }
            }
        }
    }
}
