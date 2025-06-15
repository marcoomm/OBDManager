package com.example.pruebav.ai

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

object GeminiRetrofitInstance {

    private const val BASE_URL = "https://generativelanguage.googleapis.com/"
    private const val API_KEY = Keys.apikeyGemini

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val authInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .url(chain.request().url.newBuilder().addQueryParameter("key", API_KEY).build())
            .build()
        chain.proceed(request)
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .addInterceptor(authInterceptor)
        .build()

    val api: GeminiApi = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(GeminiApi::class.java)

    interface GeminiApi {
        @POST("v1/models/gemini-1.5-flash:generateContent")
        suspend fun generateContent(
            @Body request: GeminiRequest
        ): GeminiResponse
    }

}