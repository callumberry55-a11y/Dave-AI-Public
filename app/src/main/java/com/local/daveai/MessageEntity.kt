package com.local.daveai

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    val id: String,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long,
    val persona: String,
    val extractedActionJson: String? = null,
    val generationTimeMs: Long? = null,
    val webSourcesJson: String? = null
) {
    fun toChatMessage(): ChatMessage {
        val action = if (!extractedActionJson.isNullOrBlank()) {
            try {
                Json.decodeFromString<ExtractedAction>(extractedActionJson)
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }

        val sources = if (!webSourcesJson.isNullOrBlank()) {
            try {
                Json.decodeFromString<List<WebSource>>(webSourcesJson)
            } catch (_: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }

        return ChatMessage(
            id = id,
            text = text,
            isUser = isUser,
            timestamp = timestamp,
            extractedAction = action,
            generationTimeMs = generationTimeMs,
            webSources = sources
        )
    }

    companion object {
        fun fromChatMessage(message: ChatMessage, persona: String): MessageEntity {
            val actionJson = message.extractedAction?.let {
                try {
                    Json.encodeToString(ExtractedAction.serializer(), it)
                } catch (_: Exception) {
                    null
                }
            }

            val sourcesJson = if (message.webSources.isNotEmpty()) {
                try {
                    Json.encodeToString(ListSerializer(WebSource.serializer()), message.webSources)
                } catch (_: Exception) {
                    null
                }
            } else {
                null
            }

            return MessageEntity(
                id = message.id,
                text = message.text,
                isUser = message.isUser,
                timestamp = message.timestamp,
                persona = persona,
                extractedActionJson = actionJson,
                generationTimeMs = message.generationTimeMs,
                webSourcesJson = sourcesJson
            )
        }
    }
}
