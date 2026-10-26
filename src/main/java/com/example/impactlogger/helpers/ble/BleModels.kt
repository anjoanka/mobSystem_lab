package com.example.impactlogger.helpers.ble

data class BleDevice(
    val name: String,
    val address: String,
    val rssi: Int
)

enum class BleState { Disconnected, Scanning, Connecting, Connected, Transferring }
