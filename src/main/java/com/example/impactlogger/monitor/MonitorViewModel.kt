package com.example.impactlogger.monitor

import android.app.Application
import android.hardware.SensorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.impactlogger.helpers.NetworkChecker
import com.example.impactlogger.helpers.SensorReader
import com.example.impactlogger.helpers.SmartActuator
import com.example.impactlogger.repository.CloudRepository
import com.example.impactlogger.repository.SensorData
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sqrt

class MonitorViewModel(app: Application) : AndroidViewModel(app) {

    private val sensorReader = SensorReader(app)
    private val network = NetworkChecker(app)
    private val repository = CloudRepository()
    private val actuator = SmartActuator(app)
    private val decisionEngine = DecisionEngine()

    private val _state = MutableStateFlow(
        MonitorState(
            isSensorAvailable = sensorReader.isAvailable,
            hasFlash = actuator.hasFlash,
            hasVibrator = actuator.hasVibrator
        )
    )
    val state: StateFlow<MonitorState> = _state.asStateFlow()

    private val _events = Channel<MonitorEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var sensorJob: Job? = null
    private var syncJob: Job? = null
    private var controlJob: Job? = null
    private var lastImpactTime = 0L
    private var lastAutoRelay = false

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
            is MonitorAction.SetFlashlight -> setFlashlight(action.on)
            is MonitorAction.SetRelay -> setRelay(action.on, "вручну")
            MonitorAction.TestAlarm -> activateAlarm(_state.value.gForce, "тест")
            MonitorAction.ResetAlarm -> {
                setFlashlight(false)
                _state.update { it.copy(alarmActive = false) }
                sendMessage("Тривогу скинуто")
            }
        }
    }

    private fun startSensor() {
        if (sensorJob != null) return
        _state.update { it.copy(isRunning = true) }
        sensorJob = viewModelScope.launch {
            sensorReader.readings().collect { v -> onReading(v[0], v[1], v[2]) }
        }
        syncJob = viewModelScope.launch {
            while (isActive) {
                delay(SYNC_INTERVAL_MS)
                syncToCloud()
            }
        }
        controlJob = viewModelScope.launch {
            while (isActive) {
                pollCloudCommand()
                delay(CONTROL_POLL_MS)
            }
        }
    }

    private fun stopSensor() {
        sensorJob?.cancel()
        sensorJob = null
        syncJob?.cancel()
        syncJob = null
        controlJob?.cancel()
        controlJob = null
        _state.update { it.copy(isRunning = false) }
    }

    private fun onReading(x: Float, y: Float, z: Float) {
        val g = sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH
        val threshold = _state.value.thresholdG
        val tilt = decisionEngine.tiltAngle(x, y, z)
        val decision = decisionEngine.decide(g, tilt, threshold)

        val level = when {
            decision.triggerAlarm -> ImpactLevel.Impact
            g >= (1f + threshold) / 2f -> ImpactLevel.Warning
            else -> ImpactLevel.Normal
        }

        val now = System.currentTimeMillis()
        val isNewImpact = decision.triggerAlarm && now - lastImpactTime > IMPACT_COOLDOWN_MS
        if (isNewImpact) lastImpactTime = now

        _state.update { s ->
            s.copy(
                x = x, y = y, z = z,
                gForce = g,
                level = level,
                tiltDeg = tilt,
                impacts = if (isNewImpact) {
                    (listOf(ImpactRecord(now, g)) + s.impacts).take(MAX_LOG_SIZE)
                } else {
                    s.impacts
                }
            )
        }

        if (isNewImpact) {
            repository.add(makeData("impact", g, x, y, z))
            activateAlarm(g, "удар")
        }

        if (decision.relayOn != lastAutoRelay) {
            lastAutoRelay = decision.relayOn
            setRelay(decision.relayOn, "нахил %.0f°".format(tilt))
        }
    }

    private fun activateAlarm(g: Float, reason: String) {
        actuator.triggerAlarm()
        setFlashlight(true)
        _state.update { it.copy(alarmActive = true) }
        sendMessage("ТРИВОГА ($reason): %.2f g".format(g))
    }

    private fun setFlashlight(on: Boolean) {
        if (_state.value.flashlightOn == on) return
        actuator.toggleFlashlight(on)
        _state.update { it.copy(flashlightOn = on) }
    }

    private fun setRelay(on: Boolean, reason: String) {
        if (_state.value.relayOn == on) return
        _state.update { it.copy(relayOn = on) }
        sendMessage("Реле ${if (on) "УВІМКНЕНО" else "ВИМКНЕНО"} ($reason)")
    }

    private suspend fun pollCloudCommand() {
        if (!network.isOnline()) return
        val led = repository.fetchLedStatus() ?: return
        if (led != _state.value.cloudLedOn) {
            _state.update { it.copy(cloudLedOn = led, lastCommandTime = System.currentTimeMillis()) }
            sendMessage("Команда з хмари: лампа ${if (led) "ON" else "OFF"}")
        }
    }

    private suspend fun syncToCloud() {
        val s = _state.value
        repository.add(makeData("accelerometer", s.gForce, s.x, s.y, s.z))

        if (!network.isOnline()) {
            if (s.cloudStatus != CloudStatus.Pending) {
                sendMessage("Немає інтернету. Дані збережено (Pending)")
            }
            _state.update {
                it.copy(cloudStatus = CloudStatus.Pending, pendingCount = repository.pendingCount)
            }
            return
        }

        val before = repository.pendingCount
        val ok = repository.flush()
        val sent = before - repository.pendingCount

        _state.update {
            it.copy(
                cloudStatus = if (ok) CloudStatus.Synced else CloudStatus.Error,
                sentCount = it.sentCount + sent,
                pendingCount = repository.pendingCount,
                lastSyncTime = if (ok) System.currentTimeMillis() else it.lastSyncTime
            )
        }

        if (!ok && s.cloudStatus != CloudStatus.Error) {
            sendMessage("Помилка синхронізації з хмарою")
        }
    }

    private fun makeData(type: String, value: Float, x: Float, y: Float, z: Float): SensorData {
        return SensorData(
            deviceId = DEVICE_ID,
            type = type,
            value = value,
            x = x,
            y = y,
            z = z
        )
    }

    private fun sendMessage(text: String) {
        viewModelScope.launch { _events.send(MonitorEvent.ShowMessage(text)) }
    }

    override fun onCleared() {
        actuator.toggleFlashlight(false)
        super.onCleared()
    }

    private companion object {
        const val IMPACT_COOLDOWN_MS = 500L
        const val MAX_LOG_SIZE = 20
        const val SYNC_INTERVAL_MS = 5000L
        const val CONTROL_POLL_MS = 1500L
        const val DEVICE_ID = "impact-logger-01"
    }
}
