package com.example.impactlogger.repository

import com.google.gson.annotations.SerializedName

data class SensorData(
    val deviceId: String,
    val type: String,
    val value: Float,
    val x: Float,
    val y: Float,
    val z: Float,
    val timestamp: Long = System.currentTimeMillis()
)

data class PushResponse(
    val name: String
)

data class ControlCommand(
    @SerializedName("led_status") val ledStatus: Boolean = false
)
