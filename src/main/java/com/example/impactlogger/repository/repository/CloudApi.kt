package com.example.impactlogger.repository

import com.example.impactlogger.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface CloudApi {
    @POST("sensor_data.json")
    suspend fun sendData(@Body data: SensorData): PushResponse

    @GET("control.json")
    suspend fun getControl(): Response<ControlCommand>
}

object CloudApiClient {

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
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