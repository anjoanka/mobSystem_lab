package com.example.impactlogger.monitor

enum class ImpactLevel { Normal, Warning, Impact }

enum class CloudStatus { Idle, Synced, Pending, Error }

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
    val isSensorAvailable: Boolean = true,
    val cloudStatus: CloudStatus = CloudStatus.Idle,
    val sentCount: Int = 0,
    val pendingCount: Int = 0,
    val lastSyncTime: Long? = null,
    val tiltDeg: Float = 0f,
    val alarmActive: Boolean = false,
    val relayOn: Boolean = false,
    val flashlightOn: Boolean = false,
    val cloudLedOn: Boolean = false,
    val lastCommandTime: Long? = null,
    val hasFlash: Boolean = false,
    val hasVibrator: Boolean = false
)

sealed interface MonitorAction {
    data object Start : MonitorAction
    data object Stop : MonitorAction
    data class ThresholdChanged(val value: Float) : MonitorAction
    data object ClearLog : MonitorAction
    data class SetFlashlight(val on: Boolean) : MonitorAction
    data class SetRelay(val on: Boolean) : MonitorAction
    data object TestAlarm : MonitorAction
    data object ResetAlarm : MonitorAction
}

sealed interface MonitorEvent {
    data class ShowMessage(val text: String) : MonitorEvent
}
