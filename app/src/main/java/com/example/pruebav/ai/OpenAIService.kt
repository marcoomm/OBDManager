package com.example.pruebav.ai

import com.google.gson.JsonObject

// OPEN AI
class OpenAIService {

    suspend fun sendMessage(
        messages: List<Message>
    ): Result<String> {
        return try {
            val request = OpenAIRequest(messages = messages)
            val response = RetrofitInstance.api.getChatResponse(request)

            val result = response.choices.firstOrNull()?.message?.content?.trim()
            if (result != null) {
                Result.success(result)
            } else {
                Result.failure(Exception("Respuesta vacía de OpenAI"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// GEMINI AI

class GeminiService {

    suspend fun sendMessage(prompt: String): Result<String> {
        return try {
            val content = Content(parts = listOf(Part(text = prompt)))
            val request = GeminiRequest(contents = listOf(content))
            val response = GeminiRetrofitInstance.api.generateContent(request)
            val result = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()

            if (!result.isNullOrBlank()) {
                Result.success(result)
            } else {
                Result.failure(Exception("Respuesta vacía de Gemini"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun obtenerModelosDisponibles(): Result<JsonObject> {
        return try {
            val response = GeminiRetrofitInstance.api.listModels()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error al obtener modelos: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}

