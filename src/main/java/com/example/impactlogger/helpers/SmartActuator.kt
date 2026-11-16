package com.example.impactlogger.helpers

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.VibrationEffect
import android.os.VibratorManager
import android.util.Log

class SmartActuator(context: Context) {

    private val vibrator = context.getSystemService(VibratorManager::class.java).defaultVibrator
    private val cameraManager = context.getSystemService(CameraManager::class.java)

    val hasVibrator: Boolean = vibrator.hasVibrator()
    val hasFlash: Boolean = context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)

    private val cameraId: String? = if (hasFlash) findFlashCamera() else null

    fun triggerAlarm() {
        if (!hasVibrator) {
            Log.w(TAG, "Вібромотор відсутній — тривога лише на екрані")
            return
        }
        val pattern = longArrayOf(0, 400, 200, 400)
        vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        Log.d(TAG, "Вібрація: тривога")
    }

    fun toggleFlashlight(status: Boolean): Boolean {
        val id = cameraId
        if (id == null) {
            Log.w(TAG, "Ліхтарик відсутній — віртуальний режим (${if (status) "ON" else "OFF"})")
            return false
        }
        return try {
            cameraManager.setTorchMode(id, status)
            Log.d(TAG, "Ліхтарик: ${if (status) "ON" else "OFF"}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Помилка ліхтарика: ${e.message}")
            false
        }
    }

    private fun findFlashCamera(): String? {
        return try {
            cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        const val TAG = "Actuator"
    }
}