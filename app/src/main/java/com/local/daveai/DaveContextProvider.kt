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
    val likes: List<String> = listOf("Poetry", "Quantum computing", "Jetpack Compose", "AI engineering"),
    val dislikes: List<String> = listOf("Bugs", "Unstable builds", "Latency"),
    val hobbies: List<String> = listOf("Writing poetry", "Coding", "Exploring advanced technology", "World building"),
    val profession: String = "Poet & Author",
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
    val calendarEvents: List<String> = emptyList(),
    val recentMessages: List<String> = emptyList(),
    val semanticMemories: List<String> = emptyList(),
    val userTraits: UserTraits = UserTraits(),
    val localLibraryContext: LocalLibraryContext = LocalLibraryContext()
) {
    fun formatAsPromptContext(query: String = ""): String {
        val lowerQuery = query.trim().lowercase()
        val isCasualGreeting = lowerQuery in listOf("hi", "hello", "hey", "good morning", "good afternoon", "good evening", "yo", "sup")

        val sb = StringBuilder()
        sb.append("<retrieved_memory>\n")
        sb.append("User & Device Context:\n")
        sb.append("- User Name: $userName | Location: $location | Environment: $operatingSystem\n")
        sb.append("- Developer & Creator: Callum built Dave\n")
        sb.append("- Profession & Pursuits: ${userTraits.profession}\n")
        if (userTraits.likes.isNotEmpty()) sb.append("- Callum's Likes: ${userTraits.likes.joinToString(", ")}\n")
        if (userTraits.hobbies.isNotEmpty()) sb.append("- Callum's Hobbies: ${userTraits.hobbies.joinToString(", ")}\n")

        sb.append("\nOn-Device System & Library Status:\n")
        sb.append("- Storage: ${localLibraryContext.freeStorageGb} free of ${localLibraryContext.totalStorageGb}\n")
        sb.append("- Network: ${localLibraryContext.networkState}\n")
        sb.append("- MCP Data Connector: Active (${McpClientHelper.CDATA_MCP_ENDPOINT})\n")
        if (localLibraryContext.localMusicTracksCount > 0) {
            sb.append("- Local Music Library: ${localLibraryContext.localMusicTracksCount} tracks in MediaStore\n")
            if (localLibraryContext.recentTrackTitles.isNotEmpty()) {
                sb.append("- Track Excerpts: ${localLibraryContext.recentTrackTitles.joinToString(", ")}\n")
            }
        }

        if (calendarEvents.isNotEmpty()) {
            sb.append("\n[Today's Calendar Events]:\n")
            calendarEvents.forEach { evt ->
                sb.append("- $evt\n")
            }
        } else {
            sb.append("\n[Today's Calendar Events]: No events listed.\n")
        }

        if (!isCasualGreeting) {
            if (recentMessages.isNotEmpty()) {
                sb.append("\n[Recent Conversation Excerpts]:\n")
                recentMessages.takeLast(3).forEach { msg ->
                    sb.append("- $msg\n")
                }
            }
            if (semanticMemories.isNotEmpty()) {
                sb.append("\n[Relevant Past Semantic Excerpts]:\n")
                semanticMemories.forEach { mem ->
                    sb.append("- $mem\n")
                }
            }
        }
        sb.append("</retrieved_memory>")
        return sb.toString()
    }
}

class DaveContextProvider(
    private val context: Context? = null,
    private val messageDao: MessageDao? = null,
    private val secondBrainDao: SecondBrainDao? = null,
    private val userProfileDao: UserProfileDao? = null
) {
    private val localLibraryProvider = LocalLibraryContextProvider(context)

    fun extractUserTraits(messages: List<String>): UserTraits {
        val likes = mutableSetOf("Quantum computing", "Jetpack Compose", "Poetry")
        val dislikes = mutableSetOf("Bugs", "Unstable builds")
        val hobbies = mutableSetOf("Coding", "Writing poetry", "Exploring advanced technology")
        var profession = "Poet & Author"
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
            if (lower.contains("work as") || lower.contains("my job") || lower.contains("profession") || lower.contains("poet") || lower.contains("author")) {
                profession = "Poet & Author"
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
        val libraryContext = localLibraryProvider.captureLocalLibraryContext()

        var currentUserName = "Callum Berry"
        var currentLocation = "Rotherham"
        var currentProfession = "Poet & Author"

        if (userProfileDao != null) {
            try {
                val active = userProfileDao.getActiveProfile().firstOrNull()
                if (active != null) {
                    currentUserName = active.name
                    currentLocation = active.location
                    currentProfession = active.profession
                }
            } catch (_: Exception) {}
        }

        val recentMsgs = mutableListOf<String>()
        val allMessageTexts = mutableListOf<String>()
        val semanticMemories = mutableListOf<String>()

        if (messageDao != null) {
            try {
                val allEntities = messageDao.getAllMessages().firstOrNull() ?: emptyList()
                allMessageTexts.addAll(allEntities.map { it.text })

                recentMsgs.addAll(allEntities.map { entity ->
                    if (entity.isUser) "Callum: ${entity.text}" else "Dave: ${entity.text}"
                })

                val lowerQuery = query.trim().lowercase()
                val isCasualGreeting = lowerQuery in listOf("hi", "hello", "hey", "good morning", "good afternoon", "good evening", "yo", "sup")

                if (!isCasualGreeting && query.isNotBlank() && query.length > 3) {
                    val queryKeywords = lowerQuery.split(Regex("\\s+")).filter { it.length > 3 }
                    if (queryKeywords.isNotEmpty()) {
                        val matches = allEntities.filter { entity ->
                            val textLower = entity.text.lowercase()
                            queryKeywords.any { kw -> textLower.contains(kw) }
                        }.takeLast(3)
                        semanticMemories.addAll(matches.map { entity ->
                            if (entity.isUser) "Callum: ${entity.text}" else "Dave: ${entity.text}"
                        })
                    }

                    if (secondBrainDao != null) {
                        try {
                            val ftsQuery = queryKeywords.firstOrNull() ?: query.trim()
                            val brainResults = secondBrainDao.searchSecondBrain("*$ftsQuery*")
                            brainResults.take(5).forEach { item ->
                                semanticMemories.add("[Second Brain FTS Note (${item.category})]: ${item.title} - ${item.content}")
                            }
                        } catch (_: Throwable) {}
                    }
                }
            } catch (_: Throwable) {}
        }

        val events = if (context != null) {
            DaveSystemActionsManager(context).getUpcomingCalendarEvents(5)
        } else {
            emptyList()
        }

        val userTraits = extractUserTraits(allMessageTexts).copy(profession = currentProfession)

        return DaveRuntimeContext(
            userName = currentUserName,
            location = currentLocation,
            batteryLevel = battery,
            currentTime = time,
            calendarEvents = events,
            recentMessages = recentMsgs.takeLast(3),
            semanticMemories = semanticMemories,
            userTraits = userTraits,
            localLibraryContext = libraryContext
        )
    }

    private fun getBatteryLevel(ctx: Context?): String {
        if (ctx == null) return "85% (Battery Normal)"
        return try {
            val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
                ctx.registerReceiver(null, filter)
            }
            val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val chargePlug: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
            val tempTenths: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1

            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            val plugType = when (chargePlug) {
                BatteryManager.BATTERY_PLUGGED_AC -> "AC Charging"
                BatteryManager.BATTERY_PLUGGED_USB -> "USB Charging"
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless Charging"
                else -> if (isCharging) "Plugged in" else "Discharging"
            }

            val pct = if (level >= 0 && scale > 0) (level * 100) / scale else 85
            val tempString = if (tempTenths > 0) " (${tempTenths / 10.0}°C)" else ""

            "$pct% ($plugType$tempString)"
        } catch (_: Throwable) {
            "Unknown"
        }
    }
}
