package com.example.impactlogger.monitor

import android.app.Application
import android.hardware.SensorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.impactlogger.helpers.SensorReader
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.sqrt

class MonitorViewModel(app: Application) : AndroidViewModel(app) {

    private val sensorReader = SensorReader(app)

    private val _state = MutableStateFlow(
        MonitorState(isSensorAvailable = sensorReader.isAvailable)
    )
    val state: StateFlow<MonitorState> = _state.asStateFlow()

    private val _events = Channel<MonitorEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var sensorJob: Job? = null
    private var lastImpactTime = 0L

    fun onAction(action: MonitorAction) {
        when (action) {
            MonitorAction.Start -> startSensor()
            MonitorAction.Stop -> stopSensor()
            is MonitorAction.ThresholdChanged ->
                _state.update { it.copy(thresholdG = action.value) }
            MonitorAction.ClearLog -> {
                _state.update { it.copy(impacts = emptyList()) }
                sendMessage("Журнал очищено")
            }
        }
    }

    private fun startSensor() {
        if (sensorJob != null) return
        _state.update { it.copy(isRunning = true) }
        sensorJob = viewModelScope.launch {
            sensorReader.readings().collect { v -> onReading(v[0], v[1], v[2]) }
        }
    }

    private fun stopSensor() {
        sensorJob?.cancel()
        sensorJob = null
        _state.update { it.copy(isRunning = false) }
    }

    private fun onReading(x: Float, y: Float, z: Float) {
        val g = sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH
        val threshold = _state.value.thresholdG

        val level = when {
            g >= threshold -> ImpactLevel.Impact
            g >= (1f + threshold) / 2f -> ImpactLevel.Warning
            else -> ImpactLevel.Normal
        }

        val now = System.currentTimeMillis()
        val isNewImpact = level == ImpactLevel.Impact && now - lastImpactTime > IMPACT_COOLDOWN_MS
        if (isNewImpact) lastImpactTime = now

        _state.update { s ->
            s.copy(
                x = x, y = y, z = z,
                gForce = g,
                level = level,
                impacts = if (isNewImpact) {
                    (listOf(ImpactRecord(now, g)) + s.impacts).take(MAX_LOG_SIZE)
                } else {
                    s.impacts
                }
            )
        }

        if (isNewImpact) sendMessage("Удар: %.2f g".format(g))
    }

    private fun sendMessage(text: String) {
        viewModelScope.launch { _events.send(MonitorEvent.ShowMessage(text)) }
    }

    private companion object {
        const val IMPACT_COOLDOWN_MS = 500L
        const val MAX_LOG_SIZE = 20
    }
}
