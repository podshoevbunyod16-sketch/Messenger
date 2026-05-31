package com.agon.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agon.app.data.ChatMessage
import com.agon.app.data.NovaMindRepository
import com.agon.app.data.ProviderSettings
import com.agon.app.data.STATUS_CONNECTED
import com.agon.app.data.STATUS_EMPTY
import com.agon.app.data.STATUS_ERROR
import com.agon.app.data.novaProviders
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NovaMindViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = NovaMindRepository(application.applicationContext)

    private val _settings = MutableStateFlow(ProviderSettings())
    val settings: StateFlow<ProviderSettings> = _settings.asStateFlow()

    private val _messages = MutableStateFlow(
        listOf(
            ChatMessage(
                id = 1L,
                text = "Welcome to NovaMind mobile. Connect a provider in API Keys, pick a model, then use this native chat workspace to compare responses.",
                fromUser = false,
                providerName = "NovaMind",
                modelName = "orchestrator",
            ),
            ChatMessage(
                id = 2L,
                text = "The interface mirrors the web product: dark glass cards, neon accents, provider switching, model chips and a smooth bottom-tab flow.",
                fromUser = false,
                providerName = "NovaMind",
                modelName = "mobile shell",
            ),
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking.asStateFlow()

    init {
        viewModelScope.launch {
            repository.settings.collect { _settings.value = it }
        }
    }

    fun setActiveProvider(providerId: String) {
        viewModelScope.launch { repository.setActiveProvider(providerId) }
    }

    fun saveApiKey(providerId: String, key: String) {
        viewModelScope.launch { repository.saveApiKey(providerId, key) }
    }

    fun setModel(providerId: String, model: String) {
        viewModelScope.launch { repository.setModel(providerId, model) }
    }

    fun testProvider(providerId: String) {
        viewModelScope.launch {
            val key = _settings.value.apiKeys[providerId].orEmpty()
            repository.setStatus(providerId, if (key.isBlank()) STATUS_EMPTY else STATUS_CONNECTED)
            delay(450)
            repository.setStatus(providerId, if (key.length >= 12 && !key.contains("error", ignoreCase = true)) STATUS_CONNECTED else STATUS_ERROR)
        }
    }

    fun clearKeys() {
        viewModelScope.launch { repository.clearAll() }
    }

    fun sendMessage(text: String) {
        val clean = text.trim()
        if (clean.isEmpty() || _isThinking.value) return
        val settingsSnapshot = _settings.value
        val provider = novaProviders.firstOrNull { it.id == settingsSnapshot.activeProviderId } ?: novaProviders.first()
        val model = settingsSnapshot.selectedModels[provider.id] ?: provider.models.first()
        val nextId = (System.currentTimeMillis())
        _messages.value = _messages.value + ChatMessage(nextId, clean, true, provider.name, model)
        _isThinking.value = true
        viewModelScope.launch {
            delay(850)
            val connected = settingsSnapshot.statuses[provider.id] == STATUS_CONNECTED
            val response = if (connected) {
                "${provider.name} · $model processed your request in the NovaMind mobile shell. This build stores keys locally and simulates the provider response until your backend endpoint is connected.\n\nSuggested next step: pin this provider, compare it with another free platform, or continue the conversation."
            } else {
                "${provider.name} is not connected yet. Open Settings → API Keys, paste a key, save it and run Test connection. Your message is preserved so the flow feels like the original web workspace."
            }
            _messages.value = _messages.value + ChatMessage(nextId + 1, response, false, provider.name, model)
            _isThinking.value = false
        }
    }
}
