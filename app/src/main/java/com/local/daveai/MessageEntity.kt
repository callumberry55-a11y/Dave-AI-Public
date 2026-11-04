package com.local.daveai

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    val id: String,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long,
    val persona: String,
    val attachmentUri: String? = null,
    val attachmentType: String? = null
) {
    fun toChatMessage(): ChatMessage {
        val resolvedAttachments = if (!attachmentUri.isNullOrBlank() && attachmentUri.startsWith("[")) {
            try {
                Json.decodeFromString<List<AttachmentItem>>(attachmentUri)
            } catch (_: Exception) {
                emptyList()
            }
        } else if (!attachmentUri.isNullOrBlank()) {
            listOf(AttachmentItem(attachmentUri, attachmentType?.let { AttachmentType.valueOf(it) } ?: AttachmentType.FILE, Uri.parse(attachmentUri).lastPathSegment ?: "File"))
        } else {
            emptyList()
        }

        return ChatMessage(
            id = id,
            text = text,
            isUser = isUser,
            timestamp = timestamp,
            attachments = resolvedAttachments,
            attachmentUri = attachmentUri,
            attachmentType = attachmentType?.let { AttachmentType.valueOf(it) }
        )
    }

    companion object {
        fun fromChatMessage(message: ChatMessage, persona: String): MessageEntity {
            val atts = message.getResolvedAttachments()
            val storedUri = when {
                atts.size == 1 -> atts[0].uri
                atts.size > 1 -> {
                    try {
                        Json.encodeToString(atts)
                    } catch (_: Exception) {
                        message.attachmentUri
                    }
                }
                else -> message.attachmentUri
            }
            val storedType = atts.firstOrNull()?.type?.name ?: message.attachmentType?.name

            return MessageEntity(
                id = message.id,
                text = message.text,
                isUser = message.isUser,
                timestamp = message.timestamp,
                persona = persona,
                attachmentUri = storedUri,
                attachmentType = storedType
            )
        }
    }
}
