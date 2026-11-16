package com.example.impactlogger.repository

import com.example.impactlogger.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

interface CloudApi {
    @POST("sensor_data.json")
    suspend fun sendData(@Body data: SensorData): PushResponse
}

object CloudApiClient {

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .build()

    val api: CloudApi = Retrofit.Builder()
        .baseUrl(BuildConfig.FIREBASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(CloudApi::class.java)
}