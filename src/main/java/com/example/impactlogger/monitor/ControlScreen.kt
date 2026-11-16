package com.example.impactlogger.monitor

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.impactlogger.ui.theme.ImpactLoggerTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val OnColor = Color(0xFF388E3C)
private val OffColor = Color.Gray
private val AlarmColor = Color(0xFFD32F2F)
private val LampOnColor = Color(0xFFFFC107)

@Composable
fun ControlScreen(vm: MonitorViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            when (event) {
                is MonitorEvent.ShowMessage ->
                    Toast.makeText(context, event.text, Toast.LENGTH_SHORT).show()
            }
        }
    }

    ControlContent(state = state, onAction = vm::onAction)
}

@Composable
fun ControlContent(state: MonitorState, onAction: (MonitorAction) -> Unit) {
    val timeFmt = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Керування пристроями", style = MaterialTheme.typography.headlineSmall)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Віртуальна лампа (хмара)", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.size(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(if (state.cloudLedOn) LampOnColor else OffColor, CircleShape)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("control/led_status: ${if (state.cloudLedOn) "ON" else "OFF"}")
                        val last = state.lastCommandTime?.let { timeFmt.format(Date(it)) } ?: "—"
                        Text("Остання команда: $last", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Автоматика (DecisionEngine)", style = MaterialTheme.typography.titleMedium)
                Text("Перевантаження: %.2f g (поріг %.1f g)".format(state.gForce, state.thresholdG))
                Text("Нахил: %.0f° (межа 45°)".format(state.tiltDeg))
                StatusRow("Тривога", if (state.alarmActive) "АКТИВНА" else "немає",
                    if (state.alarmActive) AlarmColor else OffColor)
                StatusRow("Реле", if (state.relayOn) "ON" else "OFF",
                    if (state.relayOn) OnColor else OffColor)
                StatusRow(
                    "Ліхтарик" + if (state.hasFlash) "" else " (віртуальний)",
                    if (state.flashlightOn) "ON" else "OFF",
                    if (state.flashlightOn) LampOnColor else OffColor
                )
                Text(
                    "Вібромотор: ${if (state.hasVibrator) "є" else "відсутній"}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Manual Control", style = MaterialTheme.typography.titleMedium)
                SwitchRow("Ліхтарик", state.flashlightOn) { onAction(MonitorAction.SetFlashlight(it)) }
                SwitchRow("Реле", state.relayOn) { onAction(MonitorAction.SetRelay(it)) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onAction(MonitorAction.TestAlarm) }) {
                        Text("Тест тривоги")
                    }
                    OutlinedButton(onClick = { onAction(MonitorAction.ResetAlarm) }) {
                        Text("Скинути тривогу")
                    }
                }
            }
        }
    }
}

@Composable
fun StatusRow(label: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, CircleShape)
        )
        Spacer(Modifier.width(8.dp))
        Text("$label: $value")
    }
}

@Composable
fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Preview(showBackground = true)
@Composable
private fun ControlContentPreview() {
    ImpactLoggerTheme {
        ControlContent(
            state = MonitorState(
                gForce = 2.4f,
                tiltDeg = 60f,
                alarmActive = true,
                relayOn = true,
                flashlightOn = true,
                cloudLedOn = true,
                lastCommandTime = System.currentTimeMillis()
            ),
            onAction = {}
        )
    }
}