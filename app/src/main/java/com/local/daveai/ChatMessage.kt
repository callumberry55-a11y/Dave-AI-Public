package com.local.daveai

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class ActionType {
    CALENDAR_EVENT,
    SEND_SMS,
    MAKE_CALL,
    PUSH_NOTIFICATION,
    LOG_NOTE,
    OPEN_APP,
    SET_ALARM,
    SET_TIMER,
    TOGGLE_FLASHLIGHT,
    SET_AUDIO_MODE,
    LAUNCHER_LAYOUT,
    OPEN_SETTINGS,
    TOGGLE_BLUETOOTH,
    TOGGLE_WIFI
}

@Serializable
data class ExtractedAction(
    val type: ActionType,
    val title: String,
    val details: String,
    val target: String? = null,
    val timeMillis: Long? = null,
    val isExecuted: Boolean = false
)

@Serializable
data class WebSource(
    val title: String,
    val url: String,
    val snippet: String = ""
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val extractedAction: ExtractedAction? = null,
    val generationTimeMs: Long? = null,
    val webSources: List<WebSource> = emptyList(),
    val attachments: List<FileAttachment> = emptyList()
)
