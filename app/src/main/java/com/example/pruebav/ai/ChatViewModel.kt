package com.example.pruebav.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {

    private val openAIService = OpenAIService()

    fun enviarMensaje(prompt: String, onResultado: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val messages = listOf(
                Message("system", "Eres un asistente experto en mantenimiento de vehículos."),
                Message("user", prompt)
            )

            val resultado = openAIService.sendMessage(messages)
            resultado
                .onSuccess { onResultado(it) }
                .onFailure { onError(it.localizedMessage ?: "Error desconocido") }
        }
    }
}
