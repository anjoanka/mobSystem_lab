package com.example.impactlogger.helpers

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

class NetworkChecker(context: Context) {

    private val manager = context.getSystemService(ConnectivityManager::class.java)

    fun isOnline(): Boolean {
        val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}