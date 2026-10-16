package com.example.intrusiondetector

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.example.intrusiondetector.ble.BleScreen
import com.example.intrusiondetector.ble.BleViewModel
import com.example.intrusiondetector.monitor.MonitorAction
import com.example.intrusiondetector.monitor.MonitorScreen
import com.example.intrusiondetector.monitor.MonitorViewModel
import com.example.intrusiondetector.ui.theme.IntrusionDetectorTheme

/**
 * Головна Activity додатку IntrusionDetector (ЛР 1 & ЛР 2).
 * Керує навігацією за допомогою вкладок та транслює події життєвого циклу у ViewModel.
 */
class MainActivity : ComponentActivity() {

    private val monitorVm: MonitorViewModel by viewModels()
    private val bleVm: BleViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            IntrusionDetectorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var selectedTabIndex by remember { mutableIntStateOf(0) }
                    val tabs = listOf("Сенсор", "Смарт-Хаб")

                    Scaffold(
                        topBar = {
                            TabRow(
                                selectedTabIndex = selectedTabIndex,
                                indicator = { tabPositions ->
                                    TabRowDefaults.SecondaryIndicator(
                                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            ) {
                                tabs.forEachIndexed { index, title ->
                                    Tab(
                                        selected = selectedTabIndex == index,
                                        onClick = { selectedTabIndex = index },
                                        text = {
                                            Text(
                                                text = title,
                                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (selectedTabIndex) {
                                0 -> MonitorScreen(vm = monitorVm)
                                1 -> BleScreen(vm = bleVm)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        monitorVm.onAction(MonitorAction.Start) // Відновлення зчитування акселерометра
    }

    override fun onPause() {
        monitorVm.onAction(MonitorAction.Stop) // Відписка для збереження батареї
        super.onPause()
    }
}
