package com.example.pruebav.ai

// OPEN AI
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



// GEMINI AI
data class Part(
    val text: String
)

data class Content(
    val parts: List<Part>
)

data class GeminiRequest(
    val contents: List<Content>
)

data class GeminiCandidate(
    val content: Content
)

data class GeminiResponse(
    val candidates: List<GeminiCandidate>
)

