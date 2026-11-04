package com.local.daveai

import android.net.Uri
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID

@Serializable
enum class AttachmentType {
    IMAGE, VIDEO, PDF, CODE, DOCUMENT, FILE
}

@Serializable
data class AttachmentItem(
    val uri: String,
    val type: AttachmentType,
    val name: String
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val attachments: List<AttachmentItem> = emptyList(),
    val attachmentUri: String? = null,
    val attachmentType: AttachmentType? = null
) {
    fun getResolvedAttachments(): List<AttachmentItem> {
        if (attachments.isNotEmpty()) return attachments
        if (attachmentUri.isNullOrBlank()) return emptyList()
        return if (attachmentUri.startsWith("[")) {
            try {
                Json.decodeFromString<List<AttachmentItem>>(attachmentUri)
            } catch (_: Exception) {
                emptyList()
            }
        } else {
            listOf(AttachmentItem(attachmentUri, attachmentType ?: AttachmentType.FILE, Uri.parse(attachmentUri).lastPathSegment ?: "File"))
        }
    }
}
