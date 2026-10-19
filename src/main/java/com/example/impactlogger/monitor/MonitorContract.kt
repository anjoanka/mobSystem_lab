package com.example.impactlogger.monitor

enum class ImpactLevel { Normal, Warning, Impact }

data class ImpactRecord(
    val timestamp: Long,
    val gForce: Float
)

data class MonitorState(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f,
    val gForce: Float = 0f,
    val level: ImpactLevel = ImpactLevel.Normal,
    val thresholdG: Float = 2.0f,
    val impacts: List<ImpactRecord> = emptyList(),
    val isRunning: Boolean = false,
    val isSensorAvailable: Boolean = true
)

sealed interface MonitorAction {
    data object Start : MonitorAction
    data object Stop : MonitorAction
    data class ThresholdChanged(val value: Float) : MonitorAction
    data object ClearLog : MonitorAction
}

sealed interface MonitorEvent {
    data class ShowMessage(val text: String) : MonitorEvent
}
