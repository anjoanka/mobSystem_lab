package com.example.impactlogger.monitor

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.impactlogger.ui.theme.ImpactLoggerTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val GRAVITY = 9.81f

@Composable
fun MonitorScreen(vm: MonitorViewModel) {
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

    MonitorContent(state = state, onAction = vm::onAction)
}

@Composable
fun MonitorContent(state: MonitorState, onAction: (MonitorAction) -> Unit) {
    val color = when (state.level) {
        ImpactLevel.Normal -> Color(0xFF388E3C)
        ImpactLevel.Warning -> Color(0xFFF9A825)
        ImpactLevel.Impact -> Color(0xFFD32F2F)
    }

    val timeFmt = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Impact Logger", style = MaterialTheme.typography.headlineMedium)

            if (!state.isSensorAvailable) {
                Text("Акселерометр недоступний", color = Color.Red)
            }

            Text(if (state.isRunning) "● Моніторинг активний" else "⏸ На паузі")

            Text("X: %.2f м/с²".format(state.x))
            Text("Y: %.2f м/с²".format(state.y))
            Text("Z: %.2f м/с²".format(state.z))

            Text(
                "Перевантаження: %.2f g".format(state.gForce),
                style = MaterialTheme.typography.titleLarge,
                color = color
            )

            LinearProgressIndicator(
                progress = { (state.gForce / 4f).coerceIn(0f, 1f) },
                color = color,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
            )

            TiltIndicator(state.x, state.y, Modifier.align(Alignment.CenterHorizontally))

            Text("Поріг удару: %.1f g".format(state.thresholdG))
            Slider(
                value = state.thresholdG,
                onValueChange = { onAction(MonitorAction.ThresholdChanged(it)) },
                valueRange = 1.1f..4f
            )

            Text(
                "Зафіксовані удари: ${state.impacts.size}",
                style = MaterialTheme.typography.titleMedium
            )
            OutlinedButton(onClick = { onAction(MonitorAction.ClearLog) }) {
                Text("Очистити журнал")
            }
            LazyColumn(Modifier.weight(1f)) {
                items(state.impacts) { record ->
                    Text("${timeFmt.format(Date(record.timestamp))} — %.2f g".format(record.gForce))
                }
            }
        }
    }
}

@Composable
fun TiltIndicator(x: Float, y: Float, modifier: Modifier = Modifier) {
    Canvas(modifier.size(150.dp)) {
        val r = size.minDimension / 2

        drawCircle(Color.Gray, radius = r, style = Stroke(4f))
        drawLine(Color.LightGray, Offset(center.x - r, center.y), Offset(center.x + r, center.y))
        drawLine(Color.LightGray, Offset(center.x, center.y - r), Offset(center.x, center.y + r))

        val dx = (-x / GRAVITY).coerceIn(-1f, 1f) * r
        val dy = (y / GRAVITY).coerceIn(-1f, 1f) * r

        drawCircle(
            Color(0xFF1976D2),
            radius = 12.dp.toPx(),
            center = Offset(center.x + dx, center.y + dy)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MonitorContentPreview() {
    ImpactLoggerTheme {
        MonitorContent(
            state = MonitorState(
                x = 3f, y = -2f, z = 9.1f,
                gForce = 2.3f,
                level = ImpactLevel.Impact,
                isRunning = true,
                impacts = listOf(
                    ImpactRecord(System.currentTimeMillis(), 2.3f),
                    ImpactRecord(System.currentTimeMillis() - 60_000, 2.8f)
                )
            ),
            onAction = {}
        )
    }
}
