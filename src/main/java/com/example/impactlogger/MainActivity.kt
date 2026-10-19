package com.example.impactlogger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.impactlogger.monitor.MonitorAction
import com.example.impactlogger.monitor.MonitorScreen
import com.example.impactlogger.monitor.MonitorViewModel
import com.example.impactlogger.ui.theme.ImpactLoggerTheme

class MainActivity : ComponentActivity() {

    private val vm: MonitorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ImpactLoggerTheme {
                MonitorScreen(vm)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vm.onAction(MonitorAction.Start)
    }

    override fun onPause() {
        vm.onAction(MonitorAction.Stop)
        super.onPause()
    }
}
