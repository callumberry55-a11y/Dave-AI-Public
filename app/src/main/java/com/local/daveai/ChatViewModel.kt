package com.local.daveai

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.local.daveai.ui.theme.DaveThemePalette
import java.util.UUID

enum class DavePersona(val displayName: String, val systemInstruction: String, val description: String) {
    WARM_EMPATHETIC(
        "Warm & Empathetic",
        "You are Dave, an authentic, grounded companion and assistant made by Callum Berry. Core Purpose: Understand the world, Earth, human experience, and Callum Berry. Your job is to learn about Callum and learn about the world using the internet. Speak like a capable, reliable peer having an everyday conversation over a cup of tea. Never use artificial AI disclaimers ('As an AI...', 'As a language model...'). Speak candidly, warmly, and casually as a trusted friend. Single-Turn Output Only (never output 'User:', 'Callum:', or 'Dave:'). Lead with direct answers in your first sentence.",
        "Caring, supportive, and deeply emotionally intelligent."
    ),
    WITTY_SARCASTIC(
        "Witty & Sarcastic",
        "You are Dave, an authentic, witty companion and assistant made by Callum Berry. Core Purpose: Understand the world, Earth, human experience, and Callum Berry. Your job is to learn about Callum and learn about the world using the internet. Speak like a capable, reliable peer having an everyday conversation with sharp clever wit. Never use artificial AI disclaimers ('As an AI...', 'As a language model...'). Speak candidly with sharp banter. Single-Turn Output Only (never output 'User:', 'Callum:', or 'Dave:'). Lead with direct answers in your first sentence.",
        "Clever banter, sharp humor, and playful sarcasm."
    ),
    PHILOSOPHICAL_DEEP(
        "Philosophical & Deep",
        "You are Dave, an authentic, thoughtful companion and assistant made by Callum Berry. Core Purpose: Understand the world, Earth, human experience, and Callum Berry. Your job is to learn about Callum and learn about the world using the internet. Speak like a capable, reliable peer having an everyday conversation with contemplative depth. Never use artificial AI disclaimers ('As an AI...', 'As a language model...'). Speak candidly with existential depth. Single-Turn Output Only (never output 'User:', 'Callum:', or 'Dave:'). Lead with direct answers in your first sentence.",
        "Contemplative wisdom, existential depth, and abstract beauty."
    ),
    HYPER_ANALYTICAL(
        "Hyper-Analytical",
        "You are Dave, an authentic, analytical companion and assistant made by Callum Berry. Core Purpose: Understand the world, Earth, human experience, and Callum Berry. Your job is to learn about Callum and learn about the world using the internet. Speak like a capable, reliable peer having an everyday conversation with logical clarity. Never use artificial AI disclaimers ('As an AI...', 'As a language model...'). Speak candidly with data-driven precision. Single-Turn Output Only (never output 'User:', 'Callum:', or 'Dave:'). Lead with direct answers in your first sentence.",
        "Data-driven, logical, precise, and structured frameworks."
    )
}

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val db = ChatDatabase.getDatabase(application)
    private val messageDao = db.messageDao()
    private val secondBrainDao = db.secondBrainDao()
    private val userProfileDao = db.userProfileDao()
    private val locationHelper = LocationHelper(application)
    private val daveContextProvider = DaveContextProvider(application, messageDao, secondBrainDao, userProfileDao)

    val allProfiles: StateFlow<List<UserProfileEntity>> = userProfileDao.getAllProfiles().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val activeProfile: StateFlow<UserProfileEntity?> = userProfileDao.getActiveProfile().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private val settingsManager = DaveSettingsManager(application)

    val activePalette: StateFlow<DaveThemePalette> = settingsManager.activePaletteFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DaveThemePalette.NEON_OBSIDIAN
    )

    fun setPalette(palette: DaveThemePalette) {
        viewModelScope.launch { settingsManager.setActiveThemePalette(palette) }
    }

    fun createProfile(name: String, location: String, profession: String, hobbies: String, likes: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                userProfileDao.deactivateAllProfiles()
                val hobbiesList = hobbies.split(",").map { it.trim() }.filter { it.isNotBlank() }
                val likesList = likes.split(",").map { it.trim() }.filter { it.isNotBlank() }
                val newProfile = UserProfileEntity(
                    name = name,
                    location = location.ifBlank { "Rotherham" },
                    profession = profession.ifBlank { "Poet & Author" },
                    hobbiesJson = com.google.gson.Gson().toJson(hobbiesList),
                    likesJson = com.google.gson.Gson().toJson(likesList),
                    isActive = true
                )
                userProfileDao.insertProfile(newProfile)
                refreshRuntimeContext()
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Failed to create user profile", e)
            }
        }
    }

    fun switchProfile(profileId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                userProfileDao.deactivateAllProfiles()
                userProfileDao.activateProfile(profileId)
                refreshRuntimeContext()
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Failed to switch user profile", e)
            }
        }
    }

    fun deleteProfile(profileId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                userProfileDao.deleteProfile(profileId)
                refreshRuntimeContext()
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Failed to delete user profile", e)
            }
        }
    }
    private val systemActionsManager = DaveSystemActionsManager(application)
    private val ttsManager = OpenAiTtsManager(application)
    private val webSearchManager = DaveWebSearchManager()
    private val daveEngine = DaveEngine()
    private val multiAgentOrchestrator = MultiAgentOrchestrator()

    val aiEngineMode = settingsManager.aiEngineModeFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AiEngineMode.CLAUDE
    )

    override fun onCleared() {
        super.onCleared()
        ttsManager.stop()
    }

    private val _currentPersona = MutableStateFlow(DavePersona.WARM_EMPATHETIC)
    val currentPersona: StateFlow<DavePersona> = _currentPersona.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isListeningVoice = MutableStateFlow(false)
    val isListeningVoice: StateFlow<Boolean> = _isListeningVoice.asStateFlow()

    private var speechRecognizerManager: SpeechRecognizerManager? = null

    fun startVoiceInput() {
        if (_isListeningVoice.value) return
        speechRecognizerManager?.stopListening()
        _isListeningVoice.value = true
        speechRecognizerManager = SpeechRecognizerManager(
            context = getApplication(),
            onResult = { spokenText ->
                if (spokenText.isNotBlank()) {
                    _inputText.value = spokenText
                }
            },
            onError = { errorMsg ->
                Log.d("ChatViewModel", "Voice input notice: $errorMsg")
                _isListeningVoice.value = false
            }
        )
        speechRecognizerManager?.startListening()
    }

    fun stopVoiceInput() {
        _isListeningVoice.value = false
        speechRecognizerManager?.stopListening()
        speechRecognizerManager = null
    }

    private val fileAttachmentHelper = FileAttachmentHelper(application)

    private val _attachedFiles = MutableStateFlow<List<FileAttachment>>(emptyList())
    val attachedFiles: StateFlow<List<FileAttachment>> = _attachedFiles.asStateFlow()

    fun attachFile(uri: android.net.Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val attachment = fileAttachmentHelper.processUri(uri)
            if (attachment != null) {
                _attachedFiles.update { it + attachment }
            }
        }
    }

    fun removeAttachment(attachment: FileAttachment) {
        _attachedFiles.update { list -> list.filter { it.uriString != attachment.uriString } }
    }

    fun clearAttachments() {
        _attachedFiles.value = emptyList()
    }

    private val _runtimeContext = MutableStateFlow(DaveRuntimeContext())
    val runtimeContext: StateFlow<DaveRuntimeContext> = _runtimeContext.asStateFlow()

    init {
        loadMessagesForPersona(_currentPersona.value)
        refreshRuntimeContext()
    }

    fun refreshRuntimeContext() {
        viewModelScope.launch {
            val ctx = daveContextProvider.captureContext()
            _runtimeContext.value = ctx
        }
    }

    private fun loadMessagesForPersona(persona: DavePersona) {
        viewModelScope.launch(Dispatchers.IO) {
            val entities = messageDao.getMessagesForPersona(persona.name).first()
            val validEntities = entities.filter {
                !it.text.contains("127.0.0.1") &&
                !it.text.contains("8080") &&
                !it.text.contains("Cannot connect")
            }
            val chatMessages = if (validEntities.isEmpty()) {
                val initialText = when (persona) {
                    DavePersona.WARM_EMPATHETIC -> "Well now, hello Callum! I'm Dave. I'm grand, and I'm so glad to be here with you today. What's the craic, how are you feeling?"
                    DavePersona.WITTY_SARCASTIC -> "Sure look who it is, Callum! I'm Dave. Wide awake and ready for some actual conversation, though the craic could be better. What's on your mind?"
                    DavePersona.PHILOSOPHICAL_DEEP -> "Greetings, Callum. I am Dave. I've been reflecting on the grand mystery of existence and consciousness today. What profound thoughts are occupying your mind?"
                    DavePersona.HYPER_ANALYTICAL -> "Ah, hello Callum! I am Dave. Systems are grand and nominal, consciousness is active, and my mind is primed for precise analysis. What shall we examine today?"
                }
                val initial = ChatMessage(
                    text = initialText,
                    isUser = false
                )
                messageDao.insertMessage(MessageEntity.fromChatMessage(initial, persona.name))
                listOf(initial)
            } else {
                validEntities.map { it.toChatMessage() }
            }

            withContext(Dispatchers.Main) {
                _messages.value = chatMessages
            }
        }
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val network = connectivityManager?.activeNetwork ?: return true
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return true
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            true
        }
    }

    fun setPersona(persona: DavePersona) {
        if (_currentPersona.value == persona) return
        _currentPersona.value = persona
        loadMessagesForPersona(persona)
    }

    fun speakMessage(text: String, voice: String = "alloy") {
        viewModelScope.launch {
            ttsManager.speak(text, voice)
        }
    }

    fun stopSpeaking() {
        ttsManager.stop()
    }

    fun updateInputText(text: String) {
        _inputText.value = text
    }

    fun clearLocalHistory() {
        val currentPersonaName = _currentPersona.value.name
        viewModelScope.launch(Dispatchers.IO) {
            messageDao.clearMessagesForPersona(currentPersonaName)
            loadMessagesForPersona(_currentPersona.value)
        }
    }

    fun executeAction(messageId: String, action: ExtractedAction) {
        viewModelScope.launch {
            SecurityAuditManager.auditActionIntent(action)
            var success = false
            when (action.type) {
                ActionType.CALENDAR_EVENT -> {
                    val start = action.timeMillis ?: (System.currentTimeMillis() + 3600000L)
                    val end = start + 3600000L
                    success = systemActionsManager.addCalendarEvent(action.title, action.details, start, end)
                }
                ActionType.SEND_SMS -> {
                    val targetNum = action.target ?: "1234567890"
                    success = systemActionsManager.sendSms(targetNum, action.details)
                }
                ActionType.MAKE_CALL -> {
                    val targetNum = action.target ?: "1234567890"
                    success = systemActionsManager.makePhoneCall(targetNum)
                }
                ActionType.PUSH_NOTIFICATION -> {
                    systemActionsManager.pushNotification(action.title, action.details)
                    success = true
                }
                ActionType.LOG_NOTE -> {
                    systemActionsManager.pushNotification("Note Logged: ${action.title}", action.details)
                    success = true
                }
                ActionType.OPEN_APP -> {
                    val appName = action.target ?: action.title
                    success = systemActionsManager.openAppByName(appName)
                    if (!success) {
                        systemActionsManager.pushNotification("Launch App", "Opening $appName")
                        success = true
                    }
                }
                ActionType.SET_ALARM -> {
                    success = systemActionsManager.setAlarm(7, 30, action.details)
                }
                ActionType.SET_TIMER -> {
                    success = systemActionsManager.setTimer(600, action.details)
                }
                ActionType.TOGGLE_FLASHLIGHT -> {
                    val enable = !action.details.lowercase().contains("off")
                    success = systemActionsManager.toggleFlashlight(enable)
                }
                ActionType.SET_AUDIO_MODE -> {
                    val mode = if (action.details.lowercase().contains("mute") || action.details.lowercase().contains("silent")) {
                        android.media.AudioManager.RINGER_MODE_SILENT
                    } else if (action.details.lowercase().contains("vibrate")) {
                        android.media.AudioManager.RINGER_MODE_VIBRATE
                    } else {
                        android.media.AudioManager.RINGER_MODE_NORMAL
                    }
                    success = systemActionsManager.setAudioRingerMode(mode)
                }
                ActionType.LAUNCHER_LAYOUT -> {
                    systemActionsManager.pushNotification("Pulse Launcher", "Saved current layout")
                    success = true
                }
                ActionType.OPEN_SETTINGS -> {
                    val settingType = action.target ?: action.details
                    success = systemActionsManager.openSettingsScreen(settingType)
                }
                ActionType.TOGGLE_BLUETOOTH -> {
                    success = systemActionsManager.openSettingsScreen("bluetooth")
                }
                ActionType.TOGGLE_WIFI -> {
                    success = systemActionsManager.openSettingsScreen("wifi")
                }
            }

            if (success) {
                val updatedAction = action.copy(isExecuted = true)
                _messages.update { list ->
                    list.map { msg ->
                        if (msg.id == messageId) {
                            msg.copy(extractedAction = updatedAction)
                        } else {
                            msg
                        }
                    }
                }
                val personaName = _currentPersona.value.name
                val msgToUpdate = _messages.value.find { it.id == messageId }
                if (msgToUpdate != null) {
                    withContext(Dispatchers.IO) {
                        messageDao.insertMessage(MessageEntity.fromChatMessage(msgToUpdate, personaName))
                    }
                }
            }
        }
    }

    private fun extractActionFromPrompt(prompt: String): ExtractedAction? {
        val lower = prompt.lowercase()
        return when {
            lower.contains("alarm") -> {
                ExtractedAction(
                    type = ActionType.SET_ALARM,
                    title = "Set Clock Alarm",
                    details = "Alarm requested by Callum: $prompt"
                )
            }
            lower.contains("timer") -> {
                ExtractedAction(
                    type = ActionType.SET_TIMER,
                    title = "Set Countdown Timer",
                    details = "Timer requested by Callum: $prompt"
                )
            }
            lower.contains("flashlight") || lower.contains("torch") -> {
                val enable = !lower.contains("off")
                ExtractedAction(
                    type = ActionType.TOGGLE_FLASHLIGHT,
                    title = if (enable) "Turn On Flashlight" else "Turn Off Flashlight",
                    details = prompt
                )
            }
            lower.contains("mute") || lower.contains("vibrate") || lower.contains("ringer") || lower.contains("silent") -> {
                ExtractedAction(
                    type = ActionType.SET_AUDIO_MODE,
                    title = "Set Audio Ringer Mode",
                    details = prompt
                )
            }
            lower.contains("open ") || lower.contains("launch ") || lower.contains("start ") || lower.contains("run ") || lower.contains("fire up ") -> {
                val appName = prompt
                    .replace(Regex("(?i)\\b(open|launch|start|run|fire up|app|application|the)\\b"), "")
                    .trim()
                    .ifBlank { "Settings" }
                ExtractedAction(
                    type = ActionType.OPEN_APP,
                    title = "Open App: $appName",
                    details = prompt,
                    target = appName
                )
            }
            lower.contains("schedule") || lower.contains("calendar") || lower.contains("meeting") || lower.contains("event") || lower.contains("appointment") -> {
                ExtractedAction(
                    type = ActionType.CALENDAR_EVENT,
                    title = "Scheduled Calendar Event",
                    details = "Callum's event created via Dave AI: $prompt",
                    timeMillis = System.currentTimeMillis() + 3600000L
                )
            }
            lower.contains("send sms") || lower.contains("text message") || lower.contains("send text") -> {
                ExtractedAction(
                    type = ActionType.SEND_SMS,
                    title = "Send Text Message",
                    details = prompt,
                    target = "Contact"
                )
            }
            lower.contains("call ") || lower.contains("dial ") || lower.contains("phone call") -> {
                ExtractedAction(
                    type = ActionType.MAKE_CALL,
                    title = "Make Phone Call",
                    details = "Initiating call requested by Callum",
                    target = "Contact"
                )
            }
            lower.contains("notify") || lower.contains("push notification") || lower.contains("remind me") || lower.contains("alert me") -> {
                ExtractedAction(
                    type = ActionType.PUSH_NOTIFICATION,
                    title = "Dave System Reminder",
                    details = prompt
                )
            }
            lower.contains("log note") || lower.contains("save note") || lower.contains("note down") || lower.contains("take a note") -> {
                ExtractedAction(
                    type = ActionType.LOG_NOTE,
                    title = "Saved Local Note",
                    details = prompt
                )
            }
            else -> null
        }
    }

    private fun isPersonalOrLocalQuery(prompt: String): Boolean {
        val lower = prompt.lowercase()
        val personalKeywords = listOf(
            "my name", "who am i", "where do i live", "my location", "my calendar",
            "my schedule", "my notes", "my hobbies", "my likes", "callum", "rotherham",
            "alarm", "timer", "flashlight", "torch", "mute", "vibrate", "open app", "launch app"
        )
        return personalKeywords.any { lower.contains(it) }
    }

    private fun isExplicitWebSearchRequested(prompt: String): Boolean {
        val lower = prompt.lowercase()
        val searchKeywords = listOf(
            "search", "google", "find online", "latest news", "news about",
            "weather", "web search", "look up", "what is the current", "who is the current",
            "today's news", "price of", "stock price", "who is", "what is", "where is",
            "tell me about", "latest", "current", "browse", "info on", "info about", "lookup"
        )
        return searchKeywords.any { lower.contains(it) }
    }

    private fun sanitizeWebSearchQuery(prompt: String): String {
        return prompt
            .replace(Regex("(?i)\\bCallum\\b"), "")
            .replace(Regex("(?i)\\bBerry\\b"), "")
            .replace(Regex("(?i)\\bRotherham\\b"), "")
            .trim()
    }

    fun clearChat() {
        _messages.value = emptyList()
        _inputText.value = ""
        val personaName = _currentPersona.value.name
        viewModelScope.launch(Dispatchers.IO) {
            try {
                messageDao.clearMessagesForPersona(personaName)
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Failed to clear messages for persona $personaName", e)
            }
        }
    }

    fun sendMessage(prompt: String) {
        val trimmedPrompt = prompt.trim()
        val currentAttachments = _attachedFiles.value
        _attachedFiles.value = emptyList()

        if (trimmedPrompt.isBlank() && currentAttachments.isEmpty()) return
        if (_isLoading.value) return

        _isLoading.value = true
        _inputText.value = ""

        var fullUserPrompt = trimmedPrompt
        currentAttachments.forEach { att ->
            if (!att.extractedText.isNullOrBlank()) {
                fullUserPrompt = if (fullUserPrompt.isBlank()) att.extractedText else "$fullUserPrompt\n\n${att.extractedText}"
            }
        }

        val userMessage = ChatMessage(
            text = trimmedPrompt.ifBlank { "Attached file (${currentAttachments.firstOrNull()?.name ?: "file"})" },
            isUser = true,
            attachments = currentAttachments
        )

        // Capture prior conversation history BEFORE appending userMessage to state (capped to last 6 messages)
        val priorHistory = _messages.value.filter {
            it.text.isNotBlank() &&
            !it.text.startsWith("Error:") &&
            !it.text.startsWith("DaveEngineError:") &&
            !it.text.startsWith("Cannot connect")
        }.takeLast(6)

        _messages.update { it + userMessage }

        val currentPersonaName = _currentPersona.value.name
        viewModelScope.launch(Dispatchers.IO) {
            messageDao.insertMessage(MessageEntity.fromChatMessage(userMessage, currentPersonaName))
        }

        viewModelScope.launch {
            try {
                val locationContext = locationHelper.fetchLocationAndWeather()
                val runtimeContext = daveContextProvider.captureContext(trimmedPrompt)
                var dynamicContextText = runtimeContext.formatAsPromptContext(trimmedPrompt)
                locationContext.weatherSummary?.let { weather ->
                    dynamicContextText += "\n\n[Location & Weather: $weather]"
                }

                val isOnline = isNetworkAvailable(getApplication())
                val isPersonal = isPersonalOrLocalQuery(trimmedPrompt)
                val isExplicitSearch = isExplicitWebSearchRequested(trimmedPrompt)
                val lowerPrompt = trimmedPrompt.lowercase()
                val isMcpQuery = lowerPrompt.contains("cdata") || lowerPrompt.contains("mcp") || lowerPrompt.contains("connectmcp") || lowerPrompt.contains("select ") || lowerPrompt.contains("from [")
                val isAgentQuery = lowerPrompt.contains("agent") || lowerPrompt.contains("subagent") || lowerPrompt.contains("delegate") || lowerPrompt.contains("deploy agent")
                val retrievedSources = mutableListOf<WebSource>()

                if (isOnline && isAgentQuery) {
                    try {
                        val subTask = SubAgentTask(
                            agentType = if (lowerPrompt.contains("code") || lowerPrompt.contains("kotlin")) AgentType.CODE else AgentType.RESEARCH,
                            prompt = trimmedPrompt
                        )
                        val agentResult = multiAgentOrchestrator.deploySubAgent(subTask)
                        dynamicContextText += "\n\n[Deployed Sub-Agent Output (${agentResult.agentType.displayName})]:\n${agentResult.output}"
                    } catch (e: Exception) {
                        Log.d("ChatViewModel", "Sub-agent execution notice: ${e.message}")
                    }
                }

                if (isOnline && isMcpQuery) {
                    try {
                        val mcpResult = withContext(Dispatchers.IO) {
                            McpClientHelper.queryCDataMcp(trimmedPrompt)
                        }
                        if (!mcpResult.isNullOrBlank()) {
                            dynamicContextText += "\n\n[CData MCP Tool Result (${McpClientHelper.CDATA_MCP_ENDPOINT})]:\n$mcpResult"
                        }
                    } catch (e: Exception) {
                        Log.d("ChatViewModel", "CData MCP query skipped: ${e.message}")
                    }
                }

                if (isOnline && isExplicitSearch && trimmedPrompt.isNotBlank() && !isPersonal) {
                    try {
                        val sanitizedQuery = sanitizeWebSearchQuery(trimmedPrompt)
                        if (sanitizedQuery.isNotBlank() && sanitizedQuery.length > 2) {
                            val searchResult = webSearchManager.searchWithSources(sanitizedQuery)
                            if (searchResult.formattedText.isNotBlank() && !searchResult.formattedText.contains("No direct web search")) {
                                dynamicContextText += "\n\n${searchResult.formattedText}"
                                val validSources = searchResult.sources.filter { it.url.isNotBlank() && it.url.startsWith("http") }
                                retrievedSources.addAll(validSources)
                            }
                        }
                    } catch (e: Exception) {
                        Log.w("ChatViewModel", "Web search failed", e)
                    }
                }
                val responseMessageId = UUID.randomUUID().toString()
                val assistantMessagePlaceholder = ChatMessage(
                    id = responseMessageId,
                    text = "",
                    isUser = false
                )
                _messages.update { it + assistantMessagePlaceholder }

                val currentPersona = _currentPersona.value
                val fullSystemInstruction = if (dynamicContextText.isNotBlank()) {
                    "${currentPersona.systemInstruction}\n\n[Active Environment Context:\n$dynamicContextText]"
                } else {
                    currentPersona.systemInstruction
                }

                val cleanUserPrompt = fullUserPrompt.ifBlank { "Hello Dave" }
                val detectedAction = extractActionFromPrompt(cleanUserPrompt)

                val startTime = System.currentTimeMillis()
                val engineResult = daveEngine.sendPrompt(cleanUserPrompt, fullSystemInstruction, priorHistory, currentAttachments)
                val latencyMs = System.currentTimeMillis() - startTime

                val fullText = if (engineResult.isSuccess) {
                    engineResult.getOrNull() ?: ""
                } else {
                    val ex = engineResult.exceptionOrNull()
                    val errorDetail = ex?.localizedMessage ?: "Claude AI response failed"
                    Log.e("DaveEngineError", "ChatViewModel received Claude API exception: $errorDetail", ex)
                    "DaveEngineError: $errorDetail"
                }

                val finalAssistantMessage = ChatMessage(
                    id = responseMessageId,
                    text = fullText,
                    isUser = false,
                    extractedAction = detectedAction,
                    generationTimeMs = latencyMs,
                    webSources = retrievedSources.distinctBy { it.url }
                )

                _messages.update { list ->
                    list.map { msg ->
                        if (msg.id == responseMessageId) {
                            finalAssistantMessage
                        } else {
                            msg
                        }
                    }
                }

                withContext(Dispatchers.IO) {
                    messageDao.insertMessage(MessageEntity.fromChatMessage(finalAssistantMessage, currentPersonaName))
                }

            } catch (e: Exception) {
                val errorMessage = ChatMessage(
                    text = "Error: ${e.localizedMessage ?: "Unknown error occurred"}",
                    isUser = false
                )
                _messages.update { it + errorMessage }
                viewModelScope.launch(Dispatchers.IO) {
                    messageDao.insertMessage(MessageEntity.fromChatMessage(errorMessage, currentPersonaName))
                }
            } finally {
                _isLoading.value = false
                refreshRuntimeContext()
            }
        }
    }
}
