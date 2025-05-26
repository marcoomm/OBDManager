package com.example.pruebav.ai


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
