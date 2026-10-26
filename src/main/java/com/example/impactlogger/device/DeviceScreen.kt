package com.example.impactlogger.device

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.impactlogger.helpers.ble.BleDevice
import com.example.impactlogger.helpers.ble.BleProfile
import com.example.impactlogger.helpers.ble.BleState
import com.example.impactlogger.ui.theme.ImpactLoggerTheme

@Composable
fun DeviceScreen(vm: DeviceViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            when (event) {
                is DeviceEvent.ShowMessage ->
                    Toast.makeText(context, event.text, Toast.LENGTH_SHORT).show()
            }
        }
    }

    DeviceContent(state = state, onAction = vm::onAction)
}

@Composable
fun DeviceContent(state: DeviceState, onAction: (DeviceAction) -> Unit) {
    val isConnected = state.bleState == BleState.Connected || state.bleState == BleState.Transferring
    val logListState = rememberLazyListState()

    LaunchedEffect(state.log.size) {
        if (state.log.isNotEmpty()) {
            logListState.animateScrollToItem(state.log.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Розумний хаб (BLE)", style = MaterialTheme.typography.headlineSmall)

        StatusIndicator(state.bleState)

        if (!isConnected) {
            Button(
                onClick = { onAction(DeviceAction.StartScan) },
                enabled = state.bleState != BleState.Scanning
            ) {
                Text("Пошук пристроїв")
            }

            if (state.bleState == BleState.Scanning) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            state.devices.forEach { device ->
                DeviceItem(device = device, onConnect = { onAction(DeviceAction.Connect(device)) })
            }
        } else {
            Text("Підключено до: ${state.connectedDevice?.name}")

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onAction(DeviceAction.SendCommand(BleProfile.CMD_PING)) }) {
                    Text("PING")
                }
                OutlinedButton(onClick = { onAction(DeviceAction.ReadStatus) }) {
                    Text("Статус")
                }
                OutlinedButton(onClick = { onAction(DeviceAction.SendCommand(BleProfile.CMD_RESET)) }) {
                    Text("RESET")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onAction(DeviceAction.SendCommand("IMPACT:2.50")) }) {
                    Text("Надіслати удар")
                }
                OutlinedButton(onClick = { onAction(DeviceAction.Disconnect) }) {
                    Text("Відключити")
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Термінал", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f))
            OutlinedButton(onClick = { onAction(DeviceAction.ClearLog) }) {
                Text("Очистити")
            }
        }

        LazyColumn(
            state = logListState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.Black)
                .padding(8.dp)
        ) {
            items(state.log) { line ->
                Text(
                    text = line,
                    color = Color.Green,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun StatusIndicator(bleState: BleState) {
    val color = when (bleState) {
        BleState.Connected, BleState.Transferring -> Color(0xFF388E3C)
        BleState.Scanning, BleState.Connecting -> Color(0xFFF9A825)
        BleState.Disconnected -> Color(0xFFD32F2F)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .background(color, CircleShape)
        )
        Spacer(Modifier.width(8.dp))
        Text("Статус: ${bleState.name}")
    }
}

@Composable
fun DeviceItem(device: BleDevice, onConnect: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(device.name, style = MaterialTheme.typography.titleMedium)
                Text("${device.address}  RSSI: ${device.rssi} dBm", fontSize = 12.sp)
            }
            Button(onClick = onConnect) {
                Text("Підключити")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DeviceContentPreview() {
    ImpactLoggerTheme {
        DeviceContent(
            state = DeviceState(
                devices = listOf(
                    BleDevice("ImpactHub-01", "AA:BB:CC:11:22:33", -58),
                    BleDevice("Mi Smart Band 7", "D4:5E:9A:40:7C:01", -81)
                ),
                log = listOf("12:00:01  Пошук пристроїв...", "12:00:03  Знайдено пристроїв: 2")
            ),
            onAction = {}
        )
    }
}
