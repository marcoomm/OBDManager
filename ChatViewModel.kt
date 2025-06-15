package com.example.pruebav.ai

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class GeminiViewModel : ViewModel() {

    private val geminiService = GeminiService()

    private val _messages = mutableStateListOf<Message>()
    val messages: SnapshotStateList<Message> get() = _messages

    fun enviarMensaje(userInput: String, onResultado: (String) -> Unit, onError: (String) -> Unit) {
        _messages.add(Message("user", userInput))

        viewModelScope.launch {
            val resultado = geminiService.sendMessage(_messages)

            resultado
                .onSuccess { respuesta ->
                    _messages.add(Message("model", respuesta))
                    onResultado(respuesta)
                }
                .onFailure { onError(it.localizedMessage ?: "Error desconocido") }
        }
    }

    fun agregarMensaje(mensaje: Message) {
        _messages.add(mensaje)
    }

}


