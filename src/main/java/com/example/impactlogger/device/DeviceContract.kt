package com.example.impactlogger.device

import com.example.impactlogger.helpers.ble.BleDevice
import com.example.impactlogger.helpers.ble.BleState

data class DeviceState(
    val bleState: BleState = BleState.Disconnected,
    val devices: List<BleDevice> = emptyList(),
    val connectedDevice: BleDevice? = null,
    val log: List<String> = emptyList()
)

sealed interface DeviceAction {
    data object StartScan : DeviceAction
    data class Connect(val device: BleDevice) : DeviceAction
    data object Disconnect : DeviceAction
    data class SendCommand(val command: String) : DeviceAction
    data object ReadStatus : DeviceAction
    data object ClearLog : DeviceAction
}

sealed interface DeviceEvent {
    data class ShowMessage(val text: String) : DeviceEvent
}
