package com.example.pruebav.ai

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST


data class Message(
    val role: String,
    val content: String
)

data class OpenAIRequest(
    val model: String = "gpt-3.5-turbo",
    val messages: List<Message>
)

data class OpenAIResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: Message
)


interface OpenAIApi {
    @POST("v1/chat/completions")
    fun getChatResponse(@Body request: OpenAIRequest): Call<OpenAIResponse>
}


object OpenAIService {

    private const val BASE_URL = "https://api.openai.com/"
    private const val API_KEY = ClaveOpenAI.API_KEY

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val authInterceptor = object : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val newRequest = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $API_KEY")
                .build()
            return chain.proceed(newRequest)
        }
    }

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .addInterceptor(authInterceptor)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .client(httpClient)
        .build()

    private val api = retrofit.create(OpenAIApi::class.java)

    fun sendMessage(
        prompt: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val messages = listOf(Message("user", prompt))
        val request = OpenAIRequest(messages = messages)

        api.getChatResponse(request).enqueue(object : Callback<OpenAIResponse> {
            override fun onResponse(call: Call<OpenAIResponse>, response: retrofit2.Response<OpenAIResponse>) {
                if (response.isSuccessful) {
                    val result = response.body()?.choices?.firstOrNull()?.message?.content
                    if (result != null) {
                        onSuccess(result.trim())
                    } else {
                        onError("Respuesta vacía.")
                    }
                } else {
                    onError("Error de servidor: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<OpenAIResponse>, t: Throwable) {
                onError("Fallo de red: ${t.localizedMessage}")
            }
        })
    }
}
