package com.example.impactlogger.repository

import android.util.Log

class CloudRepository(
    private val api: CloudApi = CloudApiClient.api
) {

    private val pending = mutableListOf<SensorData>()

    val pendingCount: Int
        get() = pending.size

    fun add(data: SensorData) {
        pending.add(data)
    }

    suspend fun flush(): Boolean {
        while (pending.isNotEmpty()) {
            val item = pending.first()
            try {
                val response = api.sendData(item)
                Log.d(TAG, "Відправлено ${item.type} (g=${item.value}), ключ: ${response.name}")
                pending.removeAt(0)
            } catch (e: Exception) {
                Log.e(TAG, "Помилка відправки: ${e.message}")
                return false
            }
        }
        return true
    }

    suspend fun fetchLedStatus(): Boolean? {
        return try {
            val response = api.getControl()
            if (response.isSuccessful) response.body()?.ledStatus else null
        } catch (e: Exception) {
            Log.e(TAG, "Помилка читання команди: ${e.message}")
            null
        }
    }

    companion object {
        const val TAG = "CloudSync"
    }
}
