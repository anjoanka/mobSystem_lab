package com.example.impactlogger.helpers.ble

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface IBleConnector {
    val state: StateFlow<BleState>

    suspend fun connect(device: BleDevice)
    fun disconnect()
    suspend fun sendData(data: String): String
    suspend fun readStatus(): String
    fun notifications(): Flow<String>
}
