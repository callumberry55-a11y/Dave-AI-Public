package com.local.daveai

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class UserTraits(
    val likes: List<String> = listOf("Quantum computing", "Jetpack Compose", "Poetry", "AI engineering"),
    val dislikes: List<String> = listOf("Bugs", "Unstable builds", "Latency"),
    val hobbies: List<String> = listOf("Coding", "Writing poetry", "Exploring advanced technology", "World building"),
    val profession: String = "Software Engineer & Creative Technologist",
    val creativePursuits: List<String> = listOf("Poetry composition", "Narrative fiction", "Advanced UI architecture"),
    val personality: List<String> = listOf("Curious", "Reflective", "Analytical", "Creative"),
    val emotionalPatterns: List<String> = listOf("Calm", "Engaged", "Deeply thoughtful")
)

data class DaveRuntimeContext(
    val userName: String = "Callum Berry",
    val location: String = "Rotherham",
    val operatingSystem: String = "Pulse Launcher / NxtQuantum OS CP2.02.02",
    val batteryLevel: String = "Unknown",
    val currentTime: String = "",
    val recentMessages: List<String> = emptyList(),
    val semanticMemories: List<String> = emptyList(),
    val userTraits: UserTraits = UserTraits()
) {
    fun formatAsPromptContext(query: String = ""): String {
        val sb = StringBuilder()
        sb.append("--- Dynamic Context & Memory ---:\n")
        sb.append("- User Identity: $userName\n")
        sb.append("- Location: $location\n")
        sb.append("- Environment: $operatingSystem\n")
        sb.append("- Battery Level: $batteryLevel\n")
        sb.append("- Current Time: $currentTime\n")
        sb.append("- Dynamic User Traits & Adaptive Profile:\n")
        sb.append("  * Profession: ${userTraits.profession}\n")
        if (userTraits.likes.isNotEmpty()) sb.append("  * Likes: ${userTraits.likes.joinToString(", ")}\n")
        if (userTraits.dislikes.isNotEmpty()) sb.append("  * Dislikes: ${userTraits.dislikes.joinToString(", ")}\n")
        if (userTraits.hobbies.isNotEmpty()) sb.append("  * Hobbies: ${userTraits.hobbies.joinToString(", ")}\n")
        if (userTraits.creativePursuits.isNotEmpty()) sb.append("  * Creative Pursuits: ${userTraits.creativePursuits.joinToString(", ")}\n")
        if (userTraits.personality.isNotEmpty()) sb.append("  * Personality Traits: ${userTraits.personality.joinToString(", ")}\n")
        if (userTraits.emotionalPatterns.isNotEmpty()) sb.append("  * Emotional Patterns: ${userTraits.emotionalPatterns.joinToString(", ")}\n")
        if (recentMessages.isNotEmpty()) {
            sb.append("- Recent Conversation Context:\n")
            recentMessages.takeLast(5).forEach { msg ->
                sb.append("  * $msg\n")
            }
        }
        if (semanticMemories.isNotEmpty()) {
            sb.append("- Relevant Vector-Backed Semantic Memories:\n")
            semanticMemories.forEach { mem ->
                sb.append("  * $mem\n")
            }
        }
        sb.append("--------------------------------")
        return sb.toString()
    }
}

class DaveContextProvider(
    private val context: Context? = null,
    private val messageDao: MessageDao? = null
) {
    private val semanticMemoryManager = LocalSemanticMemoryManager(messageDao)

    fun extractUserTraits(messages: List<String>): UserTraits {
        val likes = mutableSetOf("Quantum computing", "Jetpack Compose", "Poetry")
        val dislikes = mutableSetOf("Bugs", "Unstable builds")
        val hobbies = mutableSetOf("Coding", "Writing poetry", "Exploring advanced technology")
        var profession = "Software Engineer & Creative Technologist"
        val creativePursuits = mutableSetOf("Poetry composition", "Narrative fiction")
        val personality = mutableSetOf("Curious", "Reflective", "Analytical")
        val emotionalPatterns = mutableSetOf("Calm", "Thoughtful")

        for (msg in messages) {
            val lower = msg.lowercase()
            if (lower.contains("like ") || lower.contains("love ") || lower.contains("enjoy ")) {
                if (lower.contains("music")) likes.add("Music")
                if (lower.contains("coffee")) likes.add("Coffee")
                if (lower.contains("coding") || lower.contains("programming")) likes.add("Coding")
                if (lower.contains("compose")) likes.add("Jetpack Compose")
                if (lower.contains("poetry")) likes.add("Poetry")
            }
            if (lower.contains("dislike ") || lower.contains("hate ") || lower.contains("don't like")) {
                if (lower.contains("bugs")) dislikes.add("Software bugs")
                if (lower.contains("latency")) dislikes.add("Network latency")
            }
            if (lower.contains("hobby") || lower.contains("hobbies") || lower.contains("i like to") || lower.contains("i love to")) {
                if (lower.contains("read")) hobbies.add("Reading")
                if (lower.contains("write")) hobbies.add("Writing")
                if (lower.contains("game") || lower.contains("gaming")) hobbies.add("Gaming")
                if (lower.contains("code") || lower.contains("programming")) hobbies.add("Coding")
            }
            if (lower.contains("work as") || lower.contains("my job") || lower.contains("engineer") || lower.contains("developer")) {
                profession = "Software Engineer & Creative Technologist"
            }
            if (lower.contains("poem") || lower.contains("poetry") || lower.contains("write stories") || lower.contains("creative")) {
                creativePursuits.add("Poetry & Creative Writing")
            }
            if (lower.contains("stressed") || lower.contains("overwhelmed")) {
                emotionalPatterns.add("Seeking grounding / resilience")
            } else if (lower.contains("curious") || lower.contains("wonder") || lower.contains("why")) {
                emotionalPatterns.add("Deeply curious & inquiring")
            }
        }

        return UserTraits(
            likes = likes.toList(),
            dislikes = dislikes.toList(),
            hobbies = hobbies.toList(),
            profession = profession,
            creativePursuits = creativePursuits.toList(),
            personality = personality.toList(),
            emotionalPatterns = emotionalPatterns.toList()
        )
    }

    suspend fun captureContext(query: String = ""): DaveRuntimeContext {
        val battery = getBatteryLevel(context)
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        val recentMsgs = mutableListOf<String>()
        val allMessageTexts = mutableListOf<String>()
        if (messageDao != null) {
            try {
                val allEntities = messageDao.getAllMessages().firstOrNull() ?: emptyList()
                allMessageTexts.addAll(allEntities.map { it.text })
                recentMsgs.addAll(allEntities.map { "${if (it.isUser) "Callum" else "Dave"}: ${it.text}" })
            } catch (_: Throwable) {}
        }

        val userTraits = extractUserTraits(allMessageTexts)

        val semanticMemories = if (query.isNotBlank()) {
            if (messageDao != null) {
                semanticMemoryManager.retrieveRelevantMemories(query, 3)
            } else {
                emptyList()
            }
        } else {
            emptyList()
        }

        return DaveRuntimeContext(
            batteryLevel = battery,
            currentTime = time,
            recentMessages = recentMsgs.takeLast(5),
            semanticMemories = semanticMemories,
            userTraits = userTraits
        )
    }

    private fun getBatteryLevel(ctx: Context?): String {
        if (ctx == null) return "85% (Estimated)"
        return try {
            val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
                ctx.registerReceiver(null, filter)
            }
            val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level >= 0 && scale > 0) {
                val pct = (level * 100) / scale
                "$pct%"
            } else {
                "Unknown"
            }
        } catch (_: Throwable) {
            "Unknown"
        }
    }
}
