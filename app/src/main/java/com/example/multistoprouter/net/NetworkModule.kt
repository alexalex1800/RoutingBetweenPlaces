package com.example.multistoprouter.net

import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {
    private const val DIRECTIONS_BASE_URL = "https://maps.googleapis.com/"

    fun provideDirectionsApi(enableLogging: Boolean = false): DirectionsApi {
        val logging = HttpLoggingInterceptor().apply {
            level = if (enableLogging) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()

        val moshi = Moshi.Builder().build()

        val retrofit = Retrofit.Builder()
            .baseUrl(DIRECTIONS_BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        return retrofit.create(DirectionsApi::class.java)
    }
}
