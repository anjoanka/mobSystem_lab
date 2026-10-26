package com.example.impactlogger.device

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.impactlogger.helpers.ble.BleDevice
import com.example.impactlogger.helpers.ble.BleProfile
import com.example.impactlogger.helpers.ble.BleState
import com.example.impactlogger.helpers.ble.IBleConnector
import com.example.impactlogger.helpers.ble.MockBleConnector
import com.example.impactlogger.helpers.ble.MockBleScanner
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DeviceViewModel : ViewModel() {

    private val scanner = MockBleScanner()
    private val connector: IBleConnector = MockBleConnector()

    private val _state = MutableStateFlow(DeviceState())
    val state: StateFlow<DeviceState> = _state.asStateFlow()

    private val _events = Channel<DeviceEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var notifyJob: Job? = null
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    init {
        viewModelScope.launch {
            connector.state.collect { bleState ->
                _state.update { it.copy(bleState = bleState) }
            }
        }
    }

    fun onAction(action: DeviceAction) {
        when (action) {
            DeviceAction.StartScan -> startScan()
            is DeviceAction.Connect -> connect(action.device)
            DeviceAction.Disconnect -> disconnect()
            is DeviceAction.SendCommand -> send(action.command)
            DeviceAction.ReadStatus -> readStatus()
            DeviceAction.ClearLog -> _state.update { it.copy(log = emptyList()) }
        }
    }

    private fun startScan() {
        viewModelScope.launch {
            _state.update { it.copy(bleState = BleState.Scanning, devices = emptyList()) }
            addLog("Пошук пристроїв...")
            val found = scanner.scan()
            _state.update { it.copy(bleState = BleState.Disconnected, devices = found) }
            addLog("Знайдено пристроїв: ${found.size}")
        }
    }

    private fun connect(device: BleDevice) {
        if (device.name != BleProfile.DEVICE_NAME) {
            sendMessage("Цей пристрій не підтримується")
            return
        }
        viewModelScope.launch {
            addLog("Підключення до ${device.name}...")
            connector.connect(device)
            _state.update { it.copy(connectedDevice = device) }
            addLog("Підключено до ${device.name}")
            startNotifications()
        }
    }

    private fun startNotifications() {
        notifyJob = viewModelScope.launch {
            connector.notifications().collect { message ->
                addLog("← $message")
            }
        }
    }

    private fun disconnect() {
        notifyJob?.cancel()
        notifyJob = null
        connector.disconnect()
        _state.update { it.copy(connectedDevice = null) }
        addLog("Відключено")
    }

    private fun send(command: String) {
        if (_state.value.bleState != BleState.Connected) {
            sendMessage("Немає з'єднання")
            return
        }
        viewModelScope.launch {
            addLog("→ $command")
            val answer = connector.sendData(command)
            addLog("← $answer")
        }
    }

    private fun readStatus() {
        if (_state.value.bleState != BleState.Connected) {
            sendMessage("Немає з'єднання")
            return
        }
        viewModelScope.launch {
            addLog("→ READ STATUS")
            val status = connector.readStatus()
            addLog("← $status")
        }
    }

    private fun addLog(text: String) {
        val line = "${timeFormat.format(Date())}  $text"
        Log.d("ImpactBLE", line)
        _state.update { it.copy(log = it.log + line) }
    }

    private fun sendMessage(text: String) {
        viewModelScope.launch { _events.send(DeviceEvent.ShowMessage(text)) }
    }
}
