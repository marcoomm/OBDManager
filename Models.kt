package com.example.pruebav.ai


data class Message(
    val role: String,
    val content: String
)
data class Part(
    val text: String
)
data class Content(
    val parts: List<Part>,
    val role: String
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

