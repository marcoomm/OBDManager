package com.example.pruebav.ai


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
