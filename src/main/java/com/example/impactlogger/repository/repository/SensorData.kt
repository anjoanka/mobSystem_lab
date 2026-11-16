package com.example.impactlogger.repository

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