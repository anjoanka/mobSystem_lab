package com.example.impactlogger.helpers.ble

object BleProfile {
    const val DEVICE_NAME = "ImpactHub-01"

    const val SERVICE_UUID = "f0e10000-5a1b-4c2d-9e3f-0a1b2c3d4e5f"
    const val STATUS_UUID = "f0e10001-5a1b-4c2d-9e3f-0a1b2c3d4e5f"
    const val COMMAND_UUID = "f0e10002-5a1b-4c2d-9e3f-0a1b2c3d4e5f"
    const val IMPACT_UUID = "f0e10003-5a1b-4c2d-9e3f-0a1b2c3d4e5f"

    const val CMD_PING = "PING"
    const val CMD_GET_STATUS = "GET_STATUS"
    const val CMD_RESET = "RESET"
}
