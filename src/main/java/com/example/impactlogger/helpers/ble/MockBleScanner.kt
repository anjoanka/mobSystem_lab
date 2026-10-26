package com.example.impactlogger.helpers.ble

import android.util.Log
import kotlinx.coroutines.delay

class MockBleScanner {

    suspend fun scan(): List<BleDevice> {
        Log.d(TAG, "Сканування розпочато...")
        delay(2000)
        val devices = listOf(
            BleDevice(BleProfile.DEVICE_NAME, "AA:BB:CC:11:22:33", -58),
            BleDevice("Mi Smart Band 7", "D4:5E:9A:40:7C:01", -81)
        )
        Log.d(TAG, "Знайдено пристроїв: ${devices.size}")
        return devices
    }

    companion object {
        const val TAG = "ImpactBLE"
    }
}
