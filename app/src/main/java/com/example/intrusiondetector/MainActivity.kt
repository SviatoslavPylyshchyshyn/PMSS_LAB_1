package com.example.intrusiondetector

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.intrusiondetector.monitor.MonitorAction
import com.example.intrusiondetector.monitor.MonitorScreen
import com.example.intrusiondetector.monitor.MonitorViewModel
import com.example.intrusiondetector.ui.theme.IntrusionDetectorTheme

/**
 * Файл 4 — MainActivity.kt : показує екран і керує життєвим циклом
 * Додаток має один екран, тому onResume() і onPause() пишемо прямо в MainActivity.
 * Activity лише перетворює ці події на Action — логіки тут немає.
 */
class MainActivity : ComponentActivity() {

    // Та сама ViewModel переживає поворот екрана
    private val vm: MonitorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Увімкнення відображення на весь екран (Edge-to-Edge)
        enableEdgeToEdge()

        setContent {
            IntrusionDetectorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MonitorScreen(vm = vm)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vm.onAction(MonitorAction.Start) // Екран видно → слухаємо сенсор
    }

    override fun onPause() {
        vm.onAction(MonitorAction.Stop) // Екран сховано → відписуємось, економимо батарею
        super.onPause()
    }
}
