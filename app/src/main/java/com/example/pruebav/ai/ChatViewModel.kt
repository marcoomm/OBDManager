package com.example.pruebav.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonObject
import kotlinx.coroutines.launch

// OPEN AI
class OpenAIViewModel : ViewModel() {

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

// GEMINI AI
class GeminiViewModel : ViewModel() {

    private val geminiService = GeminiService()

    fun enviarMensaje(prompt: String, onResultado: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val resultado = geminiService.sendMessage(prompt)

            resultado
                .onSuccess { onResultado(it) }
                .onFailure { onError(it.localizedMessage ?: "Error desconocido") }
        }
    }
    fun listarModelos(onResultado: (JsonObject) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val resultado = geminiService.obtenerModelosDisponibles()
            resultado
                .onSuccess { onResultado(it) }
                .onFailure { onError(it.localizedMessage ?: "Error desconocido") }
        }
    }


}


