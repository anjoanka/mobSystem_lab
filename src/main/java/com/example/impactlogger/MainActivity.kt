package com.example.impactlogger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.impactlogger.device.DeviceScreen
import com.example.impactlogger.device.DeviceViewModel
import com.example.impactlogger.monitor.MonitorAction
import com.example.impactlogger.monitor.MonitorScreen
import com.example.impactlogger.monitor.MonitorViewModel
import com.example.impactlogger.ui.theme.ImpactLoggerTheme

class MainActivity : ComponentActivity() {

    private val monitorVm: MonitorViewModel by viewModels()
    private val deviceVm: DeviceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ImpactLoggerTheme {
                var selectedTab by rememberSaveable { mutableIntStateOf(0) }

                Scaffold(
                    topBar = {
                        TabRow(
                            selectedTabIndex = selectedTab,
                            modifier = Modifier.statusBarsPadding()
                        ) {
                            Tab(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                text = { Text("Монітор") }
                            )
                            Tab(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                text = { Text("Пристрій") }
                            )
                        }
                    }
                ) { padding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .consumeWindowInsets(padding)
                    ) {
                        when (selectedTab) {
                            0 -> MonitorScreen(monitorVm)
                            1 -> DeviceScreen(deviceVm)
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        monitorVm.onAction(MonitorAction.Start)
    }

    override fun onPause() {
        monitorVm.onAction(MonitorAction.Stop)
        super.onPause()
    }
}
