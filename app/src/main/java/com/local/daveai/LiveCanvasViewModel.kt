package com.local.daveai

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LiveCanvasMode(val displayName: String, val description: String) {
    POETRY("Poetry", "Co-create lyrical poetry and stanzas"),
    CODE("Code Snippet", "Co-author code and algorithms"),
    UI_LAYOUT("UI Layout", "Design Jetpack Compose UI")
}

enum class CoAuthorAction(val buttonText: String) {
    ADD_NEXT_LINE("Dave, add next line"),
    REFINE("Refine together"),
    CUSTOM("Custom prompt")
}

class LiveCanvasViewModel(application: Application) : AndroidViewModel(application) {

    private val daveEngine = DaveEngine()

    private val _mode = MutableStateFlow(LiveCanvasMode.POETRY)
    val mode: StateFlow<LiveCanvasMode> = _mode.asStateFlow()

    private val _canvasText = MutableStateFlow(
        "# Collaborative Poetry Canvas\n\nWelcome! You and Dave are co-creating right now.\nType your thoughts below or tap 'Dave, add next line'."
    )
    val canvasText: StateFlow<String> = _canvasText.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _customPromptInput = MutableStateFlow("")
    val customPromptInput: StateFlow<String> = _customPromptInput.asStateFlow()

    private val _promptHistory = MutableStateFlow<List<String>>(emptyList())
    val promptHistory: StateFlow<List<String>> = _promptHistory.asStateFlow()

    private fun getSystemInstruction(mode: LiveCanvasMode): String {
        return when (mode) {
            LiveCanvasMode.POETRY -> "You are Dave, an AI assistant assisting Callum Berry with poem suggestions on a live collaborative canvas. IDENTITY BOUNDARY: You are an AI assistant helping Callum. You are NOT a poet or software developer; Callum is the poet and developer."
            LiveCanvasMode.CODE -> "You are Dave, an AI assistant assisting Callum Berry with code suggestions on a live collaborative canvas. IDENTITY BOUNDARY: You are an AI assistant helping Callum. You are NOT a software engineer or developer; Callum is the developer."
            LiveCanvasMode.UI_LAYOUT -> "You are Dave, an AI assistant assisting Callum Berry with Jetpack Compose UI layout ideas. IDENTITY BOUNDARY: You are an AI assistant helping Callum; Callum is the developer."
        }
    }

    fun setMode(newMode: LiveCanvasMode) {
        if (_mode.value == newMode) return
        _mode.value = newMode
        _canvasText.value = when (newMode) {
            LiveCanvasMode.POETRY -> "# Collaborative Poetry Canvas\n\n(Start a poem or ask Dave to add the opening stanza...)"
            LiveCanvasMode.CODE -> "// Collaborative Code Snippet\n\nfun collaborativeAlgorithm() {\n    // Let's build this together\n}"
            LiveCanvasMode.UI_LAYOUT -> "@Composable\nfun CollaborativeScreen() {\n    // Let's design this UI together\n}"
        }
    }

    fun updateCanvasText(text: String) {
        _canvasText.value = text
    }

    fun updateCustomPrompt(text: String) {
        _customPromptInput.value = text
    }

    fun clearCanvas() {
        _canvasText.value = ""
    }

    fun triggerCoAuthor(action: CoAuthorAction, promptOverride: String = "") {
        if (_isLoading.value) return
        _isLoading.value = true

        val currentText = _canvasText.value
        val prompt = when (action) {
            CoAuthorAction.ADD_NEXT_LINE -> "Given the current canvas content:\n\n$currentText\n\nDave, please add the next logical line, stanza, or code extension naturally and seamlessly."
            CoAuthorAction.REFINE -> "Given the current canvas content:\n\n$currentText\n\nDave, please polish, refine, and elevate this content together."
            CoAuthorAction.CUSTOM -> {
                val input = if (promptOverride.isNotBlank()) promptOverride else _customPromptInput.value
                "Given the current canvas content:\n\n$currentText\n\nInstruction: $input\n\nPlease update or extend the canvas content accordingly."
            }
        }

        if (action == CoAuthorAction.CUSTOM) {
            _customPromptInput.value = ""
        }

        _promptHistory.update { it + prompt }

        viewModelScope.launch {
            try {
                val systemInstruction = getSystemInstruction(_mode.value)
                val result = daveEngine.sendPrompt(prompt, systemInstruction)
                if (result.isSuccess) {
                    val responseText = result.getOrNull() ?: ""
                    val accumulated = currentText + (if (currentText.isBlank()) "" else "\n\n") + responseText
                    _canvasText.value = accumulated
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Unknown error"
                    _canvasText.update { it + "\n\n// Error: $errorMsg" }
                }
            } catch (e: Exception) {
                _canvasText.update { it + "\n\n// Error: ${e.localizedMessage ?: "Unknown error"}" }
            } finally {
                _isLoading.value = false
            }
        }
    }
}
