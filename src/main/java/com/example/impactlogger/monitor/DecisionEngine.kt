package com.example.impactlogger.monitor

import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.sqrt

data class Decision(
    val triggerAlarm: Boolean,
    val relayOn: Boolean
)

class DecisionEngine(
    private val tiltLimitDeg: Float = 45f
) {

    fun tiltAngle(x: Float, y: Float, z: Float): Float {
        val length = sqrt(x * x + y * y + z * z)
        if (length == 0f) return 0f
        val cos = (z / length).coerceIn(-1f, 1f)
        return acos(cos) * 180f / PI.toFloat()
    }

    fun decide(gForce: Float, tiltDeg: Float, thresholdG: Float): Decision {
        val alarm = gForce >= thresholdG
        val relay = tiltDeg > tiltLimitDeg
        return Decision(triggerAlarm = alarm, relayOn = relay)
    }
}
