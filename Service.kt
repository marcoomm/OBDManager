package com.example.pruebav.ai


class GeminiService {

    suspend fun sendMessage(messages: List<Message>): Result<String> {
        return try {
            val contents = messages.map { message ->
                Content(
                    parts = listOf(Part(text = message.content)),
                    role = message.role
                )
            }
            val request = GeminiRequest(contents = contents)
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
}

