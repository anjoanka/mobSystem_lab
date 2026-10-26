package com.example.impactlogger.helpers.ble

import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow

class MockBleConnector : IBleConnector {

    private val _state = MutableStateFlow(BleState.Disconnected)
    override val state: StateFlow<BleState> = _state.asStateFlow()

    private var battery = 100
    private var impacts = 0

    override suspend fun connect(device: BleDevice) {
        _state.value = BleState.Connecting
        Log.d(TAG, "Підключення до ${device.name} (${device.address})")
        delay(1500)
        _state.value = BleState.Connected
        Log.d(TAG, "Підключено. Сервіс: ${BleProfile.SERVICE_UUID}")
    }

    override fun disconnect() {
        _state.value = BleState.Disconnected
        Log.d(TAG, "Відключено")
    }

    override suspend fun sendData(data: String): String {
        _state.value = BleState.Transferring
        val uuid = if (data.startsWith("IMPACT")) BleProfile.IMPACT_UUID else BleProfile.COMMAND_UUID
        Log.d(TAG, "WRITE $uuid : $data")
        delay(300)

        val answer = when {
            data == BleProfile.CMD_PING -> "PONG"
            data == BleProfile.CMD_RESET -> {
                impacts = 0
                "OK: RESET"
            }
            data.startsWith("IMPACT") -> {
                impacts++
                "ACK: impact #$impacts saved"
            }
            else -> "ERROR: unknown command"
        }

        Log.d(TAG, "Відповідь пристрою: $answer")
        _state.value = BleState.Connected
        return answer
    }

    override suspend fun readStatus(): String {
        Log.d(TAG, "READ ${BleProfile.STATUS_UUID}")
        delay(200)
        return "STATUS: battery=$battery%, impacts=$impacts"
    }

    override fun notifications(): Flow<String> = flow {
        while (true) {
            delay(3000)
            battery--
            Log.d(TAG, "NOTIFY ${BleProfile.STATUS_UUID} : battery=$battery%")
            emit("NOTIFY: battery=$battery%")
        }
    }

    companion object {
        const val TAG = "ImpactBLE"
    }
}
