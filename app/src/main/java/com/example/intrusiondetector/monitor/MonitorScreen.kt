package com.example.intrusiondetector.monitor

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.intrusiondetector.ui.components.AxisProgressBar
import com.example.intrusiondetector.ui.components.BubbleLevelCanvas
import java.util.Locale

/**
 * MonitorScreen.kt : тільки малює State і надсилає Action.
 */
@Composable
fun MonitorScreen(vm: MonitorViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // слухаємо разові Event (повідомлення, Toast)
    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            when (event) {
                is MonitorEvent.ShowMessage ->
                    Toast.makeText(context, event.text, Toast.LENGTH_SHORT).show()
            }
        }
    }

    MonitorContent(state = state, onAction = vm::onAction)
}

/**
 * Чиста Composable-функція: State на вході, Action на виході.
 * Не містить логіки або прямого доступу до Android API.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitorContent(
    state: MonitorState,
    onAction: (MonitorAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "IntrusionDetector",
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
            // картка статусу сенсора та життєвого циклу
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RectangleShape,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(RectangleShape)
                                .background(
                                    if (state.isRunning) Color(0xFF4CAF50) else Color(0xFFE53935)
                                )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = if (state.isRunning) "Сенсор: АКТИВНИЙ (Зчитування...)"
                            else "Сенсор: ЗУПИНЕНО (onPause / Енергозбереження)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Сенсор: ${state.sensorName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = String.format(
                            Locale.US,
                            "Загальне прискорення |A|: %.2f м/с²",
                            state.magnitude
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )
                }
            }

            // блок поточних координат (X, Y, Z)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RectangleShape,
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Поточні Координати Сенсора",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        CoordinateBadge(
                            axis = "X",
                            value = state.x,
                            color = Color(0xFFE53935),
                            label = "Вісь X"
                        )
                        CoordinateBadge(
                            axis = "Y",
                            value = state.y,
                            color = Color(0xFF1E88E5),
                            label = "Вісь Y"
                        )
                        CoordinateBadge(
                            axis = "Z",
                            value = state.z,
                            color = Color(0xFF43A047),
                            label = "Вісь Z"
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // прогрес-бари для кожної із осей
                    AxisProgressBar(
                        axisName = "X",
                        value = state.x,
                        color = Color(0xFFE53935),
                        description = "Вісь X"
                    )

                    AxisProgressBar(
                        axisName = "Y",
                        value = state.y,
                        color = Color(0xFF1E88E5),
                        description = "Вісь Y"
                    )

                    AxisProgressBar(
                        axisName = "Z",
                        value = state.z,
                        color = Color(0xFF43A047),
                        description = "Вісь Z"
                    )
                }
            }

            // графічний 2D Canvas індикатор положення (ватерпас)
            BubbleLevelCanvas(
                x = state.x,
                y = state.y,
                isLevel = state.isLevel
            )

            // перемикач фільтрації шумів
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RectangleShape,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Згладжування шумів (Low-Pass Filter)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (state.isFilterEnabled) "Фільтрація увімкнена" else "Сирі дані сенсора",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = state.isFilterEnabled,
                        onCheckedChange = {
                            onAction(MonitorAction.ToggleFilter)
                        }
                    )
                }
            }
        }
    }
}

/**
 * Прямокутна плашка (Surface) з цифровим значенням координати окремої осі.
 */
@Composable
private fun CoordinateBadge(
    axis: String,
    value: Float,
    color: Color,
    label: String
) {
    Surface(
        shape = RectangleShape,
        color = color.copy(alpha = 0.1f),
        modifier = Modifier.width(100.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Вісь $axis",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = String.format(Locale.US, "%+.2f", value),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = color
            )
            Text(
                text = "м/с²",
                style = MaterialTheme.typography.labelSmall,
                color = color.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
